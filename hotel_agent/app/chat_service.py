# -*- coding: utf-8 -*-
from __future__ import annotations

import logging
import json
import re
from concurrent.futures import ThreadPoolExecutor
from copy import deepcopy
from typing import Any

from .config import get_settings
from .llm_client import get_llm_client
from .prompts import ROUTER_PROMPT, SYSTEM_PROMPT
from .retrieval import get_retrieval_service
from .schemas import ChatRequest, ChatResponse, ToolExecutionRecord
from .tools import TOOL_FUNC_MAP, TOOLS

logger = logging.getLogger(__name__)

HOTEL_INTENT = "search_hotels"
ORDER_QUERY_INTENT = "query_orders"
ORDER_UPDATE_INTENT = "update_order"
ORDER_CANCEL_INTENT = "cancel_order"
KNOWLEDGE_INTENT = "knowledge_query"
GENERAL_INTENT = "general"
ROUTE_AUTO = "auto"
ROUTE_STRUCTURED = "structured"
ROUTE_RAG = "rag"
ROUTE_HYBRID = "hybrid"

HOTEL_CHAIN_KEYWORDS = ("如家", "汉庭", "全季", "亚朵", "维也纳", "锦江之星", "7天", "格林豪泰")
HOTEL_SEARCH_HINTS = ("酒店", "宾馆", "民宿", "客栈", "住宿", "旅馆", "住哪")
HOTEL_SUFFIXES = ("酒店", "宾馆", "民宿", "客栈", "公寓", "度假村")
LANDMARK_SUFFIXES = ("路", "街", "道", "巷", "里", "镇", "广场", "商圈", "地铁站", "车站", "机场", "公园", "景区", "大学")
DISTRICT_SUFFIXES = ("区", "县", "市")
CITY_SUFFIXES = ("市", "州", "地区", "盟")
ROOM_TYPE_SUFFIX = "房"
MATCH_LEVEL_EXACT = "exact"
MATCH_LEVEL_FUZZY = "fuzzy"
MATCH_LEVEL_CITY_FALLBACK = "city_fallback"
DEFAULT_FACILITY_MODE = "all"
ACTIVE_ORDER_STATUS = "booked"

FACILITY_KEYWORDS = {
    "wifi": ("wifi", "wi-fi", "无线网", "无线网络", "无线上网"),
    "breakfast": ("breakfast", "早餐", "含早", "带早餐", "提供早餐"),
    "parking": ("parking", "停车场", "可停车", "停车位", "有停车场"),
}
ORDER_QUERY_HINTS = ("订单", "预订", "入住时间", "退房时间", "离店时间", "房型", "取消", "改期", "修改")
ORDER_CANCEL_HINTS = ("取消", "撤销", "不要了")
ORDER_UPDATE_HINTS = ("修改", "更改", "改为", "改成", "换为", "换成", "调整为", "变更为", "入住时间", "退房时间", "房型")
KNOWLEDGE_QUERY_HINTS = (
    "规则",
    "政策",
    "说明",
    "介绍",
    "怎么办",
    "如何",
    "怎么",
    "可以吗",
    "能否",
    "是否",
    "几点",
    "多久",
    "收费",
    "免费",
    "押金",
    "发票",
    "宠物",
    "儿童",
    "接送",
    "入住须知",
    "退房须知",
)
KNOWLEDGE_DOMAIN_HINTS = KNOWLEDGE_QUERY_HINTS + (
    "酒店",
    "客房",
    "房间",
    "入住",
    "退房",
    "退订",
    "取消",
    "发票",
    "访客",
)
LOCATION_NOISE_WORDS = (
    "最近",
    "最新",
    "今天",
    "明天",
    "后天",
    "现在",
    "我的",
    "订单",
    "预订",
    "查询",
    "查一下",
    "查一查",
    "搜索",
    "看看",
    "帮我",
    "麻烦",
    "附近",
    "周边",
    "规则",
    "政策",
    "说明",
    "须知",
    "价格",
    "价位",
    "预算",
    "不超过",
    "以内",
    "以下",
    "以上",
    "最多",
    "至少",
    "客房",
    "使用",
    "退订",
)
QUERY_CONNECTOR_WORDS = ("有", "和", "且", "及", "以及", "并", "并且", "同时", "带", "含", "都", "又")
ROOM_TYPE_PREFIXES = ("有", "带", "含", "住", "要", "订", "查", "看")
FILTER_HOTEL_PREFIXES = ("有", "带", "含", "支持", "提供", "可", "能", "找", "查", "搜")


class ChatService:
    def __init__(self) -> None:
        self.settings = get_settings()
        self.client = None

    def chat(self, request: ChatRequest) -> ChatResponse:
        decision = self._decide_route(request)
        logger.info("chat_route message=%s decision=%s", request.message, decision)

        if decision.get("need_clarify"):
            return ChatResponse(
                intent=decision.get("intent") or GENERAL_INTENT,
                structured_data={
                    "route_type": decision.get("route_type") or ROUTE_STRUCTURED,
                    "intent": decision.get("intent") or GENERAL_INTENT,
                    "query_mode": decision.get("route_type") or ROUTE_STRUCTURED,
                    "query": {},
                    "total": 0,
                    "reply_strategy": "clarify",
                },
                reply=decision.get("clarify_question") or "请补充更多信息。",
                success=True,
                error=None,
                used_tools=[],
            )

        route_type = decision.get("route_type") or ROUTE_AUTO

        if route_type == ROUTE_STRUCTURED:
            response = self._handle_structured_route(request, decision)
            if response is not None:
                return response

        if route_type == ROUTE_RAG:
            response = self._try_handle_knowledge_request(
                request,
                query_override=decision.get("knowledge_query") or request.message,
                top_k_override=request.top_k or self.settings.rag_top_k,
            )
            if response is not None:
                return self._attach_route_type(response, ROUTE_RAG, KNOWLEDGE_INTENT, "rag_direct")

        if route_type == ROUTE_HYBRID:
            response = self._handle_hybrid_route(request, decision)
            if response is not None:
                return response

        return self._run_general_llm_flow(request)

    def _run_general_llm_flow(self, request: ChatRequest) -> ChatResponse:
        messages: list[dict[str, Any]] = [{"role": "system", "content": SYSTEM_PROMPT}]
        messages.extend(message.model_dump() for message in request.history)
        messages.append({"role": "user", "content": request.message})
        used_tools: list[ToolExecutionRecord] = []
        tool_results: list[dict[str, Any]] = []

        for _ in range(self.settings.llm_max_tool_rounds):
            response = self._get_llm_client().chat.completions.create(
                model=self.settings.llm_model,
                messages=messages,
                tools=TOOLS,
                tool_choice="auto",
            )
            assistant_message = response.choices[0].message
            tool_calls = getattr(assistant_message, "tool_calls", None) or []
            if not tool_calls:
                structured_data = self._build_structured_data(tool_results)
                intent = self._infer_intent(used_tools, request.message)
                tool_error = self._extract_tool_error(tool_results)
                reply = self._build_reply(intent, structured_data, assistant_message.content or "", tool_error)
                return ChatResponse(
                    intent=intent,
                    structured_data=self._wrap_structured_data(
                        structured_data,
                        route_type=ROUTE_AUTO,
                        intent=intent,
                        reply_strategy="general_fallback",
                    ),
                    reply=reply,
                    success=tool_error is None,
                    error=tool_error,
                    used_tools=used_tools,
                )

            messages.append(
                {
                    "role": "assistant",
                    "content": assistant_message.content or "",
                    "tool_calls": [tool_call.model_dump() for tool_call in tool_calls],
                }
            )
            for tool_call in tool_calls:
                tool_name = tool_call.function.name
                tool_args = json.loads(tool_call.function.arguments or "{}")
                tool_args = self._inject_guest_id(tool_name, tool_args, request.guest_id)
                tool_result = self._execute_tool(tool_name, tool_args)
                tool_results.append(tool_result)
                used_tools.append(
                    ToolExecutionRecord(
                        tool_name=tool_name,
                        tool_args=tool_args,
                        tool_result=tool_result,
                    )
                )
                messages.append(
                    {
                        "role": "tool",
                        "tool_call_id": tool_call.id,
                        "content": json.dumps(tool_result, ensure_ascii=False),
                    }
                )

        raise RuntimeError("Tool calling stopped because the maximum round limit was reached.")

    def _handle_structured_route(self, request: ChatRequest, decision: dict[str, Any]) -> ChatResponse | None:
        intent = decision.get("intent")

        if intent in {ORDER_QUERY_INTENT, ORDER_UPDATE_INTENT, ORDER_CANCEL_INTENT}:
            response = self._try_handle_order_request(request, action_override=intent)
            if response is not None:
                return self._attach_route_type(response, ROUTE_STRUCTURED, intent, "structured_direct")
            return None

        response = self._try_handle_hotel_search(
            request,
            query_override=self._extract_hotel_query(request.message),
        )
        if response is not None:
            return self._attach_route_type(response, ROUTE_STRUCTURED, HOTEL_INTENT, "structured_direct")
        return None

    def _handle_hybrid_route(self, request: ChatRequest, decision: dict[str, Any]) -> ChatResponse | None:
        knowledge_query = decision.get("knowledge_query") or request.message
        logger.info("hybrid_start message=%s knowledge_query=%s", request.message, knowledge_query)

        with ThreadPoolExecutor(max_workers=2) as executor:
            structured_future = executor.submit(self._handle_structured_route, request, decision)
            knowledge_future = executor.submit(
                self._try_handle_knowledge_request,
                request,
                knowledge_query,
                self.settings.rag_mix_top_k,
            )

            structured_response = structured_future.result()
            knowledge_response = knowledge_future.result()

        if structured_response is not None and knowledge_response is not None:
            return self._merge_hybrid_response(structured_response, knowledge_response, knowledge_query)

        if structured_response is not None:
            partial_response = self._attach_route_type(
                structured_response,
                ROUTE_HYBRID,
                structured_response.intent,
                "hybrid_structured_only",
            )
            partial_response.reply = f"{structured_response.reply}\n\n补充说明：当前没有检索到可用的知识库规则，先返回结构化查询结果。"
            return partial_response

        if knowledge_response is not None:
            partial_response = self._attach_route_type(
                knowledge_response,
                ROUTE_HYBRID,
                KNOWLEDGE_INTENT,
                "hybrid_rag_only",
            )
            partial_response.reply = f"结构化查询暂未命中，先为您补充相关规则说明：{knowledge_response.reply}"
            return partial_response

        return None

    def _get_llm_client(self):
        if self.client is None:
            self.client = get_llm_client()
        return self.client

    def _decide_route(self, request: ChatRequest) -> dict[str, Any]:
        route_hint = (request.route_hint or ROUTE_AUTO).strip().lower()
        if route_hint in {ROUTE_STRUCTURED, ROUTE_RAG, ROUTE_HYBRID}:
            return {
                "intent": GENERAL_INTENT,
                "route_type": route_hint,
                "confidence": 1.0,
                "need_structured": route_hint in {ROUTE_STRUCTURED, ROUTE_HYBRID},
                "need_rag": route_hint in {ROUTE_RAG, ROUTE_HYBRID},
                "knowledge_query": "",
                "need_clarify": False,
                "clarify_question": "",
                "reason": "route_hint",
            }

        hotel_query = self._extract_hotel_query(request.message)
        order_action = self._detect_order_action(request.message)
        has_hotel_condition = self._has_valid_hotel_condition(hotel_query)
        hotel_search_signal = self._looks_like_hotel_search(request.message)
        knowledge_query = self._extract_knowledge_subquery(request.message, hotel_query)
        knowledge_signal = bool(knowledge_query) or self._looks_like_knowledge_query(request.message)
        logger.info(
            "chat_parse message=%s hotel_query=%s order_action=%s knowledge_query=%s hotel_search_signal=%s has_hotel_condition=%s",
            request.message,
            hotel_query,
            order_action,
            knowledge_query,
            hotel_search_signal,
            has_hotel_condition,
        )

        # 订单优先判为结构化
        if order_action is not None:
            return {
                "intent": order_action,
                "route_type": ROUTE_STRUCTURED,
                "confidence": 0.98,
                "need_structured": True,
                "need_rag": False,
                "knowledge_query": "",
                "need_clarify": False,
                "clarify_question": "",
                "reason": "order_rule",
            }

        # 酒店搜索 + 规则说明 => hybrid
        if has_hotel_condition and knowledge_signal:
            return {
                "intent": HOTEL_INTENT,
                "route_type": ROUTE_HYBRID,
                "confidence": 0.95,
                "need_structured": True,
                "need_rag": True,
                "knowledge_query": self._build_contextual_knowledge_query(knowledge_query or request.message, hotel_query),
                "need_clarify": False,
                "clarify_question": "",
                "reason": "hotel_plus_knowledge_rule",
            }

        # 纯结构化酒店搜索
        if has_hotel_condition:
            return {
                "intent": HOTEL_INTENT,
                "route_type": ROUTE_STRUCTURED,
                "confidence": 0.95,
                "need_structured": True,
                "need_rag": False,
                "knowledge_query": "",
                "need_clarify": False,
                "clarify_question": "",
                "reason": "hotel_rule",
            }

        # “酒店 + 规则/说明”但没有结构化检索条件时，按纯知识问答处理
        if knowledge_signal:
            return {
                "intent": KNOWLEDGE_INTENT,
                "route_type": ROUTE_RAG,
                "confidence": 0.92,
                "need_structured": False,
                "need_rag": True,
                "knowledge_query": self._build_contextual_knowledge_query(knowledge_query or request.message, hotel_query),
                "need_clarify": False,
                "clarify_question": "",
                "reason": "knowledge_rule",
            }

        # 用户显然要查酒店，但没有足够条件
        if hotel_search_signal:
            return {
                "intent": HOTEL_INTENT,
                "route_type": ROUTE_STRUCTURED,
                "confidence": 0.88,
                "need_structured": True,
                "need_rag": False,
                "knowledge_query": "",
                "need_clarify": True,
                "clarify_question": "请补充至少一个检索条件，例如城市、区县、街道、酒店名、服务条件或房型。",
                "reason": "hotel_search_without_conditions",
            }

        # 规则不稳时才走 LLM 路由器
        return self._llm_route_fallback(request, hotel_query, order_action)

    def _default_route_decision(self) -> dict[str, Any]:
        return {
            "intent": GENERAL_INTENT,
            "route_type": ROUTE_AUTO,
            "confidence": 0.0,
            "need_structured": False,
            "need_rag": False,
            "knowledge_query": "",
            "need_clarify": False,
            "clarify_question": "",
            "reason": "",
        }
    
    def _extract_knowledge_subquery(self, message: str, hotel_query: dict[str, Any] | None = None) -> str:
        content = self._normalize_message(message)
        if not content:
            return ""

        rules = [
            (r"宠物|带宠物|携带宠物", "宠物入住规则"),
            (r"发票|开发票", "发票规则"),
            (r"押金", "押金规则"),
            (r"(最晚|几点|何时).{0,4}退房|退房时间", "退房时间"),
            (r"入住须知|入住规则", "入住须知"),
            (r"取消规则|取消政策|退订规则", "取消政策"),
            (r"早餐(?:时间|收费|是否免费|免费|几点|规则|政策)", "早餐规则"),
            (r"儿童", "儿童入住规则"),
        ]
        for pattern, label in rules:
            if re.search(pattern, content):
                return label

        if not self._looks_like_knowledge_query(content):
            return ""

        hotel_query = hotel_query or {}
        cleaned = content
        for value in self._flatten_query_values(hotel_query):
            cleaned = cleaned.replace(str(value), "")

        cleaned = re.sub(
            r"(帮我查|查一下|查询|找一下|看看|顺便告诉我|另外|附近|周边|酒店|宾馆|民宿|客栈|住宿|有|带|支持)",
            "",
            cleaned,
        )
        cleaned = cleaned.strip("的，。？！,.! ")
        return cleaned or content

    def _build_contextual_knowledge_query(self, knowledge_query: str, hotel_query: dict[str, Any] | None = None) -> str:
        base_query = (knowledge_query or "").strip()
        query = hotel_query or {}
        location = query.get("location") or {}
        context_parts: list[str] = []
        for key in ("city", "district", "street", "addressKeyword"):
            value = str(location.get(key) or "").strip()
            if value and value not in context_parts:
                context_parts.append(value)
        hotel_name = str(query.get("hotelName") or "").strip()
        if hotel_name:
            context_parts.append(hotel_name)
        if not context_parts:
            return base_query
        context_text = "".join(context_parts)
        if context_text and context_text not in base_query:
            return f"{context_text}{base_query}"
        return base_query


    def _try_handle_knowledge_request(
        self,
        request: ChatRequest,
        query_override: str | None = None,
        top_k_override: int | None = None,
    ) -> ChatResponse | None:
        if not self.settings.rag_enabled:
            return None

        knowledge_query = (query_override or request.message).strip()
        if not knowledge_query:
            return None

        try:
            retrieval_result = get_retrieval_service().answer(
                knowledge_query,
                hotel_id=request.hotel_id,
                doc_type=request.doc_type,
                top_k=top_k_override or request.top_k or self.settings.rag_top_k,
            )
        except Exception as exc:
            return ChatResponse(
                intent=KNOWLEDGE_INTENT,
                structured_data={
                    "route_type": ROUTE_RAG,
                    "intent": KNOWLEDGE_INTENT,
                    "query_mode": ROUTE_RAG,
                    "query": {"message": knowledge_query},
                    "knowledge": {"total": 0, "hits": [], "sources": []},
                    "sources": [],
                    "reply_strategy": "rag_error",
                },
                reply=f"知识库检索暂时不可用：{exc}",
                success=False,
                error=str(exc),
                used_tools=[],
            )

        hits = retrieval_result.get("hits") or []
        knowledge_block = {
            "total": retrieval_result.get("total", len(hits)),
            "hits": hits,
            "sources": self._extract_knowledge_sources(hits),
        }

        used_tools = [
            ToolExecutionRecord(
                tool_name="retrieve_knowledge",
                tool_args={
                    "message": knowledge_query,
                    "hotel_id": request.hotel_id,
                    "doc_type": request.doc_type,
                    "top_k": top_k_override or request.top_k or self.settings.rag_top_k,
                },
                tool_result=retrieval_result,
            )
        ]

        return ChatResponse(
            intent=KNOWLEDGE_INTENT,
            structured_data={
                "route_type": ROUTE_RAG,
                "intent": KNOWLEDGE_INTENT,
                "query_mode": ROUTE_RAG,
                "query": {
                    "message": knowledge_query,
                    "hotel_id": request.hotel_id,
                    "doc_type": request.doc_type,
                },
                "knowledge": knowledge_block,
                "sources": knowledge_block["sources"],
                "reply_strategy": "rag_direct",
            },
            reply=retrieval_result.get("answer") or "已为您检索到相关知识片段。",
            success=retrieval_result.get("ok", True),
            error=None,
            used_tools=used_tools,
        )

    def _merge_hybrid_response(
        self,
        structured_response: ChatResponse,
        knowledge_response: ChatResponse,
        knowledge_query: str,
    ) -> ChatResponse:
        merged_structured_data = deepcopy(structured_response.structured_data or {})
        merged_structured_data["route_type"] = ROUTE_HYBRID
        merged_structured_data["intent"] = structured_response.intent
        merged_structured_data["query_mode"] = ROUTE_HYBRID
        merged_structured_data["knowledge"] = (knowledge_response.structured_data or {}).get("knowledge", {})
        merged_structured_data["sources"] = (knowledge_response.structured_data or {}).get("sources", [])
        merged_structured_data["reply_strategy"] = "hybrid_template"

        base_query = merged_structured_data.get("query") or {}
        if isinstance(base_query, dict):
            base_query["knowledge_query"] = knowledge_query
            merged_structured_data["query"] = base_query

        structured_reply = (structured_response.reply or "").strip()
        knowledge_reply = (knowledge_response.reply or "").strip()

        structured_has_result = self._has_structured_payload(structured_response.structured_data or {})
        knowledge_has_result = self._has_knowledge_payload(knowledge_response.structured_data or {})

        if structured_has_result and knowledge_reply:
            merged_reply = f"{structured_reply}\n\n规则说明：{knowledge_reply}"
        elif knowledge_has_result and knowledge_reply:
            merged_reply = f"结构化查询暂未命中，先为您补充相关规则说明：{knowledge_reply}"
        elif structured_reply:
            merged_reply = f"{structured_reply}\n\n补充说明：当前没有检索到可用的知识库规则。"
        else:
            merged_reply = structured_reply or knowledge_reply or "已为您完成查询。"

        merged_used_tools = list(structured_response.used_tools)
        merged_used_tools.extend(knowledge_response.used_tools)

        return ChatResponse(
            intent=structured_response.intent,
            structured_data=merged_structured_data,
            reply=merged_reply,
            success=structured_response.success or knowledge_response.success,
            error=structured_response.error or knowledge_response.error,
            used_tools=merged_used_tools,
        )

    @staticmethod
    def _has_structured_payload(structured_data: dict[str, Any]) -> bool:
        if not structured_data:
            return False
        return bool(
            structured_data.get("hotels")
            or structured_data.get("orders")
            or structured_data.get("candidates")
            or structured_data.get("reservation")
            or structured_data.get("order")
            or structured_data.get("matched")
        )

    @staticmethod
    def _has_knowledge_payload(structured_data: dict[str, Any]) -> bool:
        knowledge = (structured_data or {}).get("knowledge") or {}
        return bool(knowledge.get("total") or knowledge.get("hits") or knowledge.get("sources"))

    def _wrap_structured_data(
        self,
        structured_data: dict[str, Any],
        *,
        route_type: str,
        intent: str,
        reply_strategy: str,
    ) -> dict[str, Any]:
        next_data = deepcopy(structured_data or {})
        next_data["route_type"] = route_type
        next_data["intent"] = intent
        next_data["query_mode"] = route_type
        next_data["reply_strategy"] = reply_strategy
        next_data.setdefault("sources", [])
        return next_data

    def _attach_route_type(
        self,
        response: ChatResponse,
        route_type: str,
        intent: str,
        reply_strategy: str,
    ) -> ChatResponse:
        response.structured_data = self._wrap_structured_data(
            response.structured_data or {},
            route_type=route_type,
            intent=intent,
            reply_strategy=reply_strategy,
        )
        return response

    @staticmethod
    def _attach_route_marker(structured_data: dict[str, Any], route_type: str) -> dict[str, Any]:
        next_structured_data = deepcopy(structured_data or {})
        next_structured_data["route_type"] = route_type
        return next_structured_data

    @staticmethod
    def _extract_knowledge_sources(hits: list[dict[str, Any]]) -> list[dict[str, Any]]:
        sources: list[dict[str, Any]] = []
        seen: set[str] = set()
        for hit in hits:
            source_name = str(hit.get("source") or hit.get("file_name") or "").strip()
            if not source_name or source_name in seen:
                continue
            seen.add(source_name)
            sources.append(
                {
                    "source": source_name,
                    "file_name": hit.get("file_name") or "",
                    "doc_type": hit.get("doc_type") or "",
                    "hotel_id": hit.get("hotel_id") or "",
                }
            )
        return sources

    def _try_handle_hotel_search(
        self,
        request: ChatRequest,
        query_override: dict[str, Any] | None = None,
    ) -> ChatResponse | None:
        if self._looks_like_order_request(request.message):
            return None
        query = self._compact_query_object(query_override or self._extract_hotel_query(request.message))
        if self._has_valid_hotel_condition(query):
            tool_results = self._search_hotels_with_fallback(query)
            used_tools = [ToolExecutionRecord(tool_name=HOTEL_INTENT, tool_args=result.get("raw_tool_args") or query, tool_result=result) for result in tool_results]
            selected_result = tool_results[-1] if tool_results else {}
            structured_data = self._build_structured_data(tool_results, preferred_query=query)
            tool_error = self._extract_tool_error(tool_results)
            reply = self._build_hotel_reply(structured_data, tool_error, selected_result.get("matchLevel"))
            logger.info(
                "chat_hotel_result rawQuery=%s intent=%s slots=%s finalRequest=%s hitCount=%s replyTemplateType=%s failureReason=%s",
                request.message,
                HOTEL_INTENT,
                query,
                [result.get("raw_tool_args") for result in tool_results],
                structured_data.get("total", 0),
                self._detect_hotel_reply_template_type(structured_data, tool_error, selected_result.get("matchLevel")),
                tool_error or "",
            )
            return ChatResponse(intent=HOTEL_INTENT, structured_data=structured_data, reply=reply, success=tool_error is None, error=tool_error, used_tools=used_tools)
        if self._looks_like_hotel_search(request.message):
            return ChatResponse(intent=HOTEL_INTENT, structured_data=self._empty_hotel_structured_data(), reply="请告诉我至少一个检索条件，例如城市、区县、地标、酒店名、服务条件或房型，我就可以直接为您查询。", success=True, error=None, used_tools=[])
        return None

    def _try_handle_order_request(
        self,
        request: ChatRequest,
        action_override: str | None = None,
    ) -> ChatResponse | None:
        action = action_override or self._detect_order_action(request.message)
        if action is None:
            return None
        if request.guest_id is None:
            return ChatResponse(intent=action, structured_data={}, reply="未能识别当前登录用户，请先登录后再查询订单。", success=False, error="GUEST_ID_MISSING", used_tools=[])

        order_query = self._extract_order_query(request.message, request.guest_id, action, request.order_candidates)
        if action == ORDER_QUERY_INTENT:
            if self._looks_like_recent_order_only_request(request.message, order_query):
                tool_args = {"guest_id": request.guest_id, "limit": order_query.get("limit")}
                tool_result = self._execute_tool("get_recent_orders", tool_args)
            else:
                tool_args = {"filters": order_query, "guest_id": request.guest_id}
                tool_result = self._execute_tool("search_orders", tool_args)
        elif action == ORDER_UPDATE_INTENT:
            order_updates = self._extract_order_updates(request.message)
            logger.info("order_update_parse message=%s query=%s updates=%s", request.message, order_query, order_updates)
            if not order_updates:
                return ChatResponse(intent=ORDER_UPDATE_INTENT, structured_data={"query": order_query, "matched": False, "multiple": False, "candidates": [], "order": {}}, reply="请告诉我想修改的内容，例如新的入住时间、退房时间或房型。", success=True, error=None, used_tools=[])
            tool_args = {"filters": order_query, "updates": order_updates, "guest_id": request.guest_id}
            tool_result = self._execute_tool("update_order_by_query", tool_args)
        else:
            tool_args = {"filters": order_query, "reason": self._extract_cancel_reason(request.message), "guest_id": request.guest_id}
            tool_result = self._execute_tool("cancel_order_by_query", tool_args)

        structured_data = self._build_structured_data([tool_result])
        tool_error = self._extract_tool_error([tool_result])
        reply = self._build_reply(action, structured_data, "", tool_error)
        logger.info(
            "chat_order_result rawQuery=%s intent=%s filters=%s updates=%s finalRequest=%s hitCount=%s replyTemplateType=%s failureReason=%s",
            request.message,
            action,
            order_query,
            order_updates if action == ORDER_UPDATE_INTENT else {},
            tool_args,
            structured_data.get("total", 0),
            self._detect_order_reply_template_type(action, structured_data, tool_error),
            tool_error or "",
        )
        return ChatResponse(intent=action, structured_data=structured_data, reply=reply, success=tool_error is None, error=tool_error, used_tools=[ToolExecutionRecord(tool_name=tool_result.get("tool") or action, tool_args=tool_args, tool_result=tool_result)])
    def _search_hotels_with_fallback(self, query: dict[str, Any]) -> list[dict[str, Any]]:
        attempts = self._build_hotel_search_attempts(query)
        tool_results: list[dict[str, Any]] = []
        for attempt in attempts:
            tool_result = self._execute_tool(HOTEL_INTENT, attempt["tool_args"])
            tool_result["raw_tool_args"] = deepcopy(attempt["tool_args"])
            tool_result["query"] = self._compact_query_object(attempt["response_query"])
            tool_result["matchLevel"] = attempt["matchLevel"]
            tool_results.append(tool_result)
            if not tool_result.get("ok"):
                break
            if tool_result.get("count", 0) > 0:
                return tool_results
        return tool_results or [{"ok": True, "tool": HOTEL_INTENT, "summary": "", "count": 0, "hotels": [], "query": self._compact_query_object(query), "raw_tool_args": deepcopy(query), "matchLevel": MATCH_LEVEL_EXACT}]

    def _build_hotel_search_attempts(self, query: dict[str, Any]) -> list[dict[str, Any]]:
        exact_query = deepcopy(query)
        exact_location = exact_query.get("location") or {}
        if exact_location:
            exact_location.pop("districtKeyword", None)
            if not exact_location.get("district") and query.get("location", {}).get("districtKeyword"):
                exact_location["district"] = query["location"]["districtKeyword"]
            exact_query["location"] = self._compact_query_object(exact_location)

        fuzzy_query = deepcopy(query)
        fuzzy_location = fuzzy_query.get("location") or {}
        if fuzzy_location.get("district") and not fuzzy_location.get("districtKeyword"):
            fuzzy_location["districtKeyword"] = fuzzy_location["district"]
        # fuzzy 阶段不要再保留强约束的精确 district / street，避免把“周边/附近”查成精确地址过滤
        fuzzy_location.pop("district", None)
        if fuzzy_location.get("addressKeyword"):
            fuzzy_location.pop("street", None)
        fuzzy_query["location"] = self._compact_query_object(fuzzy_location)

        city_fallback_query = self._build_city_fallback_query(query)
        attempts: list[dict[str, Any]] = []
        if self._has_valid_hotel_condition(exact_query):
            attempts.append({"matchLevel": MATCH_LEVEL_EXACT, "tool_args": exact_query, "response_query": exact_query})
        if self._should_add_fuzzy_attempt(query, exact_query, fuzzy_query):
            attempts.append({"matchLevel": MATCH_LEVEL_FUZZY, "tool_args": fuzzy_query, "response_query": fuzzy_query})
        if city_fallback_query is not None and city_fallback_query != exact_query and city_fallback_query != fuzzy_query:
            attempts.append({"matchLevel": MATCH_LEVEL_CITY_FALLBACK, "tool_args": city_fallback_query, "response_query": city_fallback_query})
        return attempts

    @staticmethod
    def _should_add_fuzzy_attempt(original_query: dict[str, Any], exact_query: dict[str, Any], fuzzy_query: dict[str, Any]) -> bool:
        if fuzzy_query == exact_query:
            return False
        location = original_query.get("location") or {}
        return any(location.get(key) for key in ("districtKeyword", "district", "street", "addressKeyword")) or bool(original_query.get("hotelName"))

    def _build_city_fallback_query(self, query: dict[str, Any]) -> dict[str, Any] | None:
        location = query.get("location") or {}
        city = location.get("city")
        if not city:
            return None
        fallback_query = deepcopy(query)
        fallback_location = {"city": city}
        if location.get("province"):
            fallback_location["province"] = location["province"]
        fallback_query["location"] = fallback_location
        return self._compact_query_object(fallback_query)

    def _execute_tool(self, tool_name: str, tool_args: dict[str, Any]) -> dict[str, Any]:
        tool_func = TOOL_FUNC_MAP.get(tool_name)
        if tool_func is None:
            return {"ok": False, "tool": tool_name, "error": {"message": f"Tool not found: {tool_name}"}}
        try:
            return tool_func(**tool_args)
        except Exception as exc:  # noqa: BLE001
            return {"ok": False, "tool": tool_name, "error": {"message": str(exc)}}

    @staticmethod
    def _inject_guest_id(tool_name: str, tool_args: dict[str, Any], guest_id: int | None) -> dict[str, Any]:
        if guest_id is None:
            return tool_args
        if tool_name in {"get_recent_orders", "get_order_detail", "update_order", "cancel_order"} and tool_args.get("guest_id") is None:
            tool_args["guest_id"] = guest_id
        if tool_name in {"search_orders", "update_order_by_query", "cancel_order_by_query"}:
            tool_args.setdefault("guest_id", guest_id)
            filters = tool_args.get("filters") or {}
            if isinstance(filters, dict) and filters.get("guestId") is None:
                filters["guestId"] = guest_id
                tool_args["filters"] = filters
        return tool_args

    @staticmethod
    def _flatten_query_values(data: Any) -> list[Any]:
        values: list[Any] = []
        if isinstance(data, dict):
            for value in data.values():
                values.extend(ChatService._flatten_query_values(value))
        elif isinstance(data, list):
            for item in data:
                values.extend(ChatService._flatten_query_values(item))
        elif data not in (None, "", [], {}):
            values.append(data)
        return values

    def _infer_intent(self, used_tools: list[ToolExecutionRecord], message: str) -> str:
        if used_tools:
            tool_name = used_tools[-1].tool_name
            return {"search_orders": ORDER_QUERY_INTENT, "get_recent_orders": ORDER_QUERY_INTENT, "update_order_by_query": ORDER_UPDATE_INTENT, "cancel_order_by_query": ORDER_CANCEL_INTENT}.get(tool_name, tool_name)
        if self._looks_like_hotel_search(message):
            return HOTEL_INTENT
        order_action = self._detect_order_action(message)
        if order_action is not None:
            return order_action
        return GENERAL_INTENT

    def _build_structured_data(self, tool_results: list[dict[str, Any]], preferred_query: dict[str, Any] | None = None) -> dict[str, Any]:
        if not tool_results:
            return {}
        result = tool_results[-1]
        tool_name = result.get("tool")
        if tool_name == HOTEL_INTENT:
            return {"query": result.get("query") or self._compact_query_object(preferred_query or {}), "total": result.get("count", 0), "hotels": result.get("hotels", []), "match_level": result.get("matchLevel")}
        if tool_name in {"search_orders", "get_recent_orders"}:
            return {"query": result.get("query") or {}, "total": result.get("total", result.get("count", 0)), "orders": result.get("orders", [])}
        if tool_name == "get_order_detail":
            return {"reservation": result.get("order")}
        if tool_name in {"update_order", "update_order_by_query", "cancel_order", "cancel_order_by_query"}:
            order_payload = result.get("order") if self._has_meaningful_order_payload(result.get("order")) else None
            structured_data = {
                "query": result.get("query") or {},
                "total": result.get("total", len(result.get("candidates", []) or [])),
                "matched": result.get("matched", result.get("ok", False)),
                "multiple": result.get("multiple", False),
                "candidates": result.get("candidates", []),
            }
            if order_payload is not None:
                structured_data["order"] = order_payload
                structured_data["reservation"] = order_payload
            return structured_data
        return result

    @staticmethod
    def _extract_tool_error(tool_results: list[dict[str, Any]]) -> str | None:
        for result in reversed(tool_results):
            if not result.get("ok"):
                error = result.get("error") or {}
                return error.get("message") or "Tool execution failed."
        return None

    def _build_reply(self, intent: str, structured_data: dict[str, Any], assistant_content: str, tool_error: str | None) -> str:
        if intent == HOTEL_INTENT:
            return self._build_hotel_reply(structured_data, tool_error, structured_data.get("match_level"))
        if intent == ORDER_QUERY_INTENT:
            return self._build_order_query_reply(structured_data, tool_error)
        if intent in {ORDER_UPDATE_INTENT, ORDER_CANCEL_INTENT}:
            return self._build_order_action_reply(intent, structured_data, tool_error)
        content = (assistant_content or "").strip()
        return content or self._default_reply(intent, structured_data)

    def _build_hotel_reply(self, structured_data: dict[str, Any], tool_error: str | None = None, match_level: str | None = None) -> str:
        if tool_error:
            return f"酒店查询失败：{tool_error}"
        total = structured_data.get("total", 0)
        query = structured_data.get("query") or {}
        location = query.get("location") or {}
        query_summary = self._summarize_hotel_query(query)
        if total <= 0:
            if query_summary:
                return f"暂时没有查到符合“{query_summary}”的酒店。您可以继续补充更具体的地址、服务条件或房型要求，我再帮您缩小范围。"
            return "暂时没有查到符合条件的酒店。您可以继续补充更具体的地址、服务条件或房型要求，我再帮您缩小范围。"
        if match_level == MATCH_LEVEL_FUZZY:
            if query_summary:
                return f"没有找到完全精准匹配“{query_summary}”的结果，先为您展示 {total} 家相关酒店。您可以继续补充更具体的街道、服务或房型要求。"
            return f"没有找到完全精准匹配的结果，先为您展示 {total} 家相关酒店。您可以继续补充更具体的街道、服务或房型要求。"
        if match_level == MATCH_LEVEL_CITY_FALLBACK:
            city = location.get("city") or "当前城市"
            if query_summary:
                return f"没有找到更精准的区域结果，先为您展示 {city} 范围内与“{query_summary}”相关的 {total} 家酒店。您可以继续补充区县、服务条件或房型要求。"
            return f"没有找到更精准的区域结果，先为您展示 {city} 范围内的 {total} 家酒店。您可以继续补充区县、服务条件或房型要求。"
        if query_summary:
            return f"已为您找到 {total} 家符合“{query_summary}”的酒店，列表已经展开。您还可以继续补充地址、酒店服务或房型条件。"
        return f"已为您找到 {total} 家符合条件的酒店，列表已经展开。您还可以继续补充地址、酒店服务或房型条件。"

    def _build_order_query_reply(self, structured_data: dict[str, Any], tool_error: str | None = None) -> str:
        if tool_error == "GUEST_ID_MISSING":
            return "未能识别当前登录用户，请先登录后再查询订单。"
        if tool_error:
            return f"订单查询失败：{tool_error}"
        total = structured_data.get("total", 0)
        query = structured_data.get("query") or {}
        query_summary = self._summarize_order_query(query)
        requested_limit = query.get("limit")
        if total <= 0:
            if query_summary:
                return f"没有找到符合“{query_summary}”的订单。您可以补充酒店名、城市、区县或时间范围后再试。"
            return "没有找到符合条件的订单。您可以补充酒店名、城市、区县或时间范围后再试。"
        if requested_limit:
            return f"已为您返回最近 {total} 条订单，候选列表已展示。您可以继续指定其中某一条进行修改或取消。"
        if query_summary:
            return f"已为您找到 {total} 条符合“{query_summary}”的订单，候选列表已展示。您可以继续指定其中某一条进行修改或取消。"
        return f"已为您找到 {total} 条订单，候选列表已展示。您可以继续指定其中某一条进行修改或取消。"

    def _build_order_action_reply(self, intent: str, structured_data: dict[str, Any], tool_error: str | None = None) -> str:
        if tool_error == "GUEST_ID_MISSING":
            return "未能识别当前登录用户，请先登录后再查询订单。"
        if tool_error:
            prefix = "订单修改失败" if intent == ORDER_UPDATE_INTENT else "订单取消失败"
            return f"{prefix}：{tool_error}"
        if structured_data.get("multiple"):
            return "为您找到多条相关订单，请先确认具体要处理哪一条。"
        if not structured_data.get("matched"):
            return "没有找到符合条件的订单。您可以补充更具体的酒店名、地点或时间范围。"
        hotel_name = (structured_data.get("order") or {}).get("hotelName") or "目标酒店"
        return f"已为您完成 {hotel_name} 订单的{'修改' if intent == ORDER_UPDATE_INTENT else '取消'}，最新结果已展示。"

    @staticmethod
    def _detect_hotel_reply_template_type(structured_data: dict[str, Any], tool_error: str | None, match_level: str | None) -> str:
        if tool_error:
            return "hotel_error"
        if (structured_data or {}).get("total", 0) <= 0:
            return "hotel_empty"
        if match_level == MATCH_LEVEL_FUZZY:
            return "hotel_fuzzy"
        if match_level == MATCH_LEVEL_CITY_FALLBACK:
            return "hotel_city_fallback"
        return "hotel_success"

    @staticmethod
    def _detect_order_reply_template_type(intent: str, structured_data: dict[str, Any], tool_error: str | None) -> str:
        if tool_error:
            return f"{intent}_error"
        if (structured_data or {}).get("multiple"):
            return f"{intent}_multiple"
        if not (structured_data or {}).get("matched", False) and intent != ORDER_QUERY_INTENT:
            return f"{intent}_not_found"
        if intent == ORDER_QUERY_INTENT and (structured_data or {}).get("total", 0) <= 0:
            return "query_orders_empty"
        return f"{intent}_success"

    @staticmethod
    def _summarize_hotel_query(query: dict[str, Any]) -> str:
        next_query = query or {}
        location = next_query.get("location") or {}
        facilities = (next_query.get("facilities") or {}).get("required") or []
        room_type = next_query.get("roomType") or {}
        parts: list[str] = []
        area = "".join(str(location.get(key) or "").strip() for key in ("city", "district", "street"))
        if area:
            parts.append(area)
        hotel_name = str(next_query.get("hotelName") or "").strip()
        if hotel_name:
            parts.append(hotel_name)
        facility_labels = {
            "wifi": "WiFi",
            "breakfast": "早餐",
            "parking": "停车",
        }
        facility_text = "、".join(facility_labels.get(item, str(item)) for item in facilities if item)
        if facility_text:
            parts.append(facility_text)
        room_type_keyword = str(room_type.get("roomTypeKeyword") or "").strip()
        if room_type_keyword:
            parts.append(room_type_keyword)
        return " / ".join(parts)

    @staticmethod
    def _summarize_order_query(query: dict[str, Any]) -> str:
        next_query = query or {}
        parts: list[str] = []
        if next_query.get("hotelName"):
            parts.append(str(next_query["hotelName"]))
        area = "".join(str(next_query.get(key) or "").strip() for key in ("city", "district"))
        if area:
            parts.append(area)
        if next_query.get("status"):
            parts.append(str(next_query["status"]))
        if next_query.get("recentDays"):
            parts.append(f"最近{next_query['recentDays']}天")
        return " / ".join(parts)

    @staticmethod
    def _default_reply(intent: str, structured_data: dict[str, Any]) -> str:
        if intent == ORDER_QUERY_INTENT:
            return f"已为您查询到 {structured_data.get('total', 0)} 条订单。"
        if intent == "get_order_detail":
            return "已为您查询到订单详情。"
        if intent == KNOWLEDGE_INTENT:
            return "已为您检索到相关知识内容。"
        if intent == ORDER_UPDATE_INTENT:
            return "订单修改已处理。"
        if intent == ORDER_CANCEL_INTENT:
            return "订单取消已处理。"
        return "已收到您的问题。"
    def _extract_hotel_query(self, message: str) -> dict[str, Any]:
        content = self._strip_request_prefix(self._normalize_message(message))
        if not content:
            return {}
        location = self._extract_location_tokens(content)
        facilities = self._extract_facilities(content)
        room_type = self._extract_room_type(content)
        hotel_name = self._extract_hotel_name(content, location)
        min_price, max_price = self._extract_price_range(content)
        rating = self._extract_rating(content)
        query: dict[str, Any] = {}
        if location:
            query["location"] = location
        if facilities:
            query["facilities"] = facilities
        if room_type:
            query["roomType"] = room_type
        if hotel_name:
            query["hotelName"] = hotel_name
        if min_price is not None:
            query["minPrice"] = min_price
        if max_price is not None:
            query["maxPrice"] = max_price
        if rating is not None:
            query["rating"] = rating
        return self._compact_query_object(query)

    def _extract_order_query(self, message: str, guest_id: int, action: str, order_candidates: list[dict[str, Any]] | None = None) -> dict[str, Any]:
        content = self._strip_request_prefix(self._normalize_message(message))
        location = self._extract_location_tokens(content)
        hotel_name = self._extract_hotel_name(content, location)
        recent_days = self._extract_recent_days(content)
        explicit_limit = self._extract_order_limit(content)
        limit = explicit_limit or (1 if any(keyword in content for keyword in ("最新订单", "最近预订", "最近一笔", "最近的订单")) else 10)
        status = self._extract_order_status(content)
        if action in {ORDER_UPDATE_INTENT, ORDER_CANCEL_INTENT} and not status:
            status = ACTIVE_ORDER_STATUS
        query = self._compact_query_object({
            "guestId": guest_id,
            "reservationId": self._extract_explicit_order_id(content),
            "recentDays": recent_days,
            "province": location.get("province"),
            "city": location.get("city"),
            "district": location.get("district") or location.get("districtKeyword"),
            "hotelName": hotel_name,
            "roomTypeKeyword": self._extract_source_order_room_type(content),
            "status": status,
            "sort": "createdAt_desc",
            "limit": limit,
        })
        candidate_query = self._resolve_candidate_order_query(content, guest_id, order_candidates or [])
        if candidate_query:
            query.update(candidate_query)
        return self._compact_query_object(query)

    def _extract_order_updates(self, message: str) -> dict[str, Any]:
        content = self._normalize_message(message)
        updates: dict[str, Any] = {}
        check_in_match = re.search(r"(?:入住时间|预计入住时间)(?:改为|改成|改到|调整为|变更为|为)(\d{4}[/-]\d{1,2}[/-]\d{1,2}|\d{4}年\d{1,2}月\d{1,2}日)", content)
        check_out_match = re.search(r"(?:退房时间|离店时间|预计退房时间)(?:改为|改成|改到|调整为|变更为|为)(\d{4}[/-]\d{1,2}[/-]\d{1,2}|\d{4}年\d{1,2}月\d{1,2}日)", content)
        if check_in_match:
            updates["checkInDate"] = self._normalize_date_token(check_in_match.group(1))
        if check_out_match:
            updates["checkOutDate"] = self._normalize_date_token(check_out_match.group(1))

        room_type_keyword = None
        pair_match = re.search(r"([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)(?:改为|改成|换成|换为|调整为|改到|换到|变更为|为)([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)", content)
        if pair_match:
            room_type_keyword = pair_match.group(2)
        else:
            for pattern in (
                r"(?:房型|房间类型)(?:改为|改成|换成|换为|调整为|改到|换到|变更为|为)([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)",
                r"(?:改为|改成|换成|换为|调整为|改到|换到|变更为)([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)",
            ):
                room_type_match = re.search(pattern, content)
                if room_type_match:
                    room_type_keyword = room_type_match.group(1)
                    break
        if room_type_keyword:
            updates["roomTypeKeyword"] = room_type_keyword
        return self._compact_query_object(updates)

    def _extract_location_tokens(self, content: str) -> dict[str, Any]:
        location_content = self._preprocess_location_content(content)
        location: dict[str, Any] = {}
        province_match = re.search(r"([\u4e00-\u9fa5]{2,12}(?:省|自治区|特别行政区))", location_content)
        city_match = re.search(r"([\u4e00-\u9fa5]{2,12}(?:市|州|地区|盟))", location_content)
        district_match = re.search(r"([\u4e00-\u9fa5]{2,12}(?:区|县|市))", location_content)
        street_match = re.search(r"([\u4e00-\u9fa5A-Za-z0-9]{2,20}(?:路|街|道|巷|里|镇))", location_content)
        landmark_match = re.search(r"([\u4e00-\u9fa5A-Za-z0-9]{2,20}(?:广场|商圈|地铁站|车站|机场|公园|景区|大学))", location_content)
        if province_match:
            location["province"] = self._normalize_location_token(province_match.group(1))
        if city_match:
            location["city"] = self._normalize_location_token(city_match.group(1))
        if district_match and not self._looks_like_city(district_match.group(1)):
            district = self._normalize_location_token(district_match.group(1))
            for key in ("province", "city"):
                prefix = location.get(key)
                if prefix and district.startswith(prefix):
                    district = district[len(prefix):]
            location.setdefault("district", district)
            location.setdefault("districtKeyword", district)
        if street_match:
            street = self._normalize_location_token(street_match.group(1))
            location.setdefault("street", street)
            location.setdefault("addressKeyword", street)
        if landmark_match:
            location.setdefault("addressKeyword", self._normalize_location_token(landmark_match.group(1)))

        location_candidate = self._extract_location_candidate(location_content, location)
        if location_candidate:
            if self._looks_like_city(location_candidate):
                location.setdefault("city", location_candidate)
            elif self._looks_like_district(location_candidate):
                location.setdefault("district", location_candidate)
                location.setdefault("districtKeyword", location_candidate)
            elif self._looks_like_street_or_landmark(location_candidate):
                if self._looks_like_street(location_candidate):
                    location.setdefault("street", location_candidate)
                location.setdefault("addressKeyword", location_candidate)
            elif not any(location.get(key) for key in ("city", "district", "addressKeyword")) and self._looks_like_loose_location_candidate(location_candidate):
                location.setdefault("city", location_candidate)
        return self._compact_query_object(location)

    def _extract_location_candidate(self, content: str, location: dict[str, Any]) -> str | None:
        cleaned = self._preprocess_location_content(content)
        cleaned = re.sub(r"^(?:请|帮我|帮忙|给我|替我|我想|我要|麻烦你)", "", cleaned)
        cleaned = re.sub(r"^(?:查一下|查一查|查询|查找|搜索|搜一下|看一下|看看|推荐)", "", cleaned)
        cleaned = re.sub(r"(?:酒店|宾馆|民宿|客栈|住宿|旅馆|订单|预订)", "", cleaned)
        cleaned = re.sub(r"(?:附近|周边|的)", "", cleaned)
        cleaned = re.sub(r"(?i)wifi|wi-fi|breakfast|parking", "", cleaned)
        cleaned = cleaned.replace("无线网络", "").replace("无线网", "").replace("含早", "").replace("早餐", "").replace("停车场", "")
        cleaned = re.sub(r"(?:价格|价位|预算|金额)(?:不超过|不高于|低于|小于|高于|大于|至少|最多|以内|以下|以上|为|在)?", "", cleaned)
        cleaned = re.sub(r"(?:不超过|不高于|低于|小于|最多|至多|以内|以下|不低于|至少|高于|大于)\d+(?:\.\d+)?元?", "", cleaned)
        cleaned = re.sub(r"\d+(?:\.\d+)?元?(?:以下|以内|以上|起|起价|左右)?", "", cleaned)
        cleaned = re.sub(r"([\u4e00-\u9fa5A-Za-z0-9]{1,8}房)", "", cleaned)
        cleaned = self._strip_connector_words(cleaned)
        cleaned = cleaned.strip("，。？！,.! ")
        if not cleaned:
            return None
        for value in location.values():
            if value:
                cleaned = cleaned.replace(str(value), "")
        cleaned = self._strip_connector_words(cleaned)
        cleaned = cleaned.strip("，。？！,.! ")
        if len(cleaned) < 2 or self._is_noise_location_candidate(cleaned) or re.search(r"\d", cleaned):
            return None
        return self._normalize_location_token(cleaned)

    def _extract_hotel_name(self, content: str, location: dict[str, Any]) -> str | None:
        hotel_name_content = self._preprocess_location_content(content)
        match = re.search(r"([\u4e00-\u9fa5A-Za-z0-9]{2,24}(?:酒店|宾馆|民宿|客栈|公寓|度假村))", hotel_name_content)
        if match:
            candidate = self._strip_location_prefix(match.group(1), location)
            stem = candidate
            for suffix in HOTEL_SUFFIXES:
                if stem.endswith(suffix):
                    stem = stem[: -len(suffix)]
                    break
            if stem and not any(value == stem for value in location.values()) and not self._looks_like_generic_hotel_token(stem):
                return candidate
        for keyword in HOTEL_CHAIN_KEYWORDS:
            if keyword in hotel_name_content:
                return keyword if keyword.endswith(HOTEL_SUFFIXES) else f"{keyword}酒店"
        return None

    def _strip_location_prefix(self, hotel_name: str, location: dict[str, Any]) -> str:
        candidate = (hotel_name or "").strip().strip("的")
        for key in ("province", "city", "district", "districtKeyword", "street", "addressKeyword"):
            value = str(location.get(key) or "").strip()
            if value and candidate.startswith(value):
                candidate = candidate[len(value):].strip().strip("的")
        return candidate or hotel_name

    @staticmethod
    def _is_noise_location_candidate(candidate: str) -> bool:
        cleaned = (candidate or "").strip()
        if not cleaned:
            return True
        return any(word in cleaned for word in LOCATION_NOISE_WORDS)

    def _looks_like_recent_order_only_request(self, message: str, order_query: dict[str, Any]) -> bool:
        content = self._strip_request_prefix(self._normalize_message(message))
        explicit_limit = self._extract_order_limit(content)
        has_recent_signal = any(keyword in content for keyword in ("最近订单", "最近的订单", "最新订单", "最近预订", "最新预订", "最近一笔"))
        if not has_recent_signal and not ("最近" in content and explicit_limit is not None):
            return False
        meaningful_keys = {key for key, value in (order_query or {}).items() if value not in (None, "", [], {})}
        return meaningful_keys.issubset({"guestId", "limit", "sort"})

    @staticmethod
    def _looks_like_generic_hotel_token(token: str) -> bool:
        cleaned = (token or "").strip().strip("的")
        if not cleaned:
            return True
        if cleaned in {"酒店", "宾馆", "民宿", "客栈"}:
            return True
        if any(word in cleaned for word in ("周边", "附近", "这里", "那个", "这个")):
            return True
        if re.search(r"\d", cleaned):
            return True
        if any(word in cleaned for word in ("价格", "价位", "预算", "不超过", "以内", "以下", "以上", "至少", "最多")):
            return True
        if "的" in cleaned and any(cleaned.startswith(prefix) for prefix in FILTER_HOTEL_PREFIXES):
            return True
        normalized = cleaned.lower()
        return any(keyword in normalized for keyword in ("wifi", "wi-fi", "无线网", "无线网络", "无线上网", "早餐", "停车", "大床房", "双床房", "标间", "套房"))

    @staticmethod
    def _normalize_facility_text(content: str) -> str:
        normalized = (content or "").lower()
        normalized = normalized.replace("wi-fi", "wifi").replace("wi fi", "wifi").replace(" ", "")
        return normalized

    def _extract_facilities(self, content: str) -> dict[str, Any] | None:
        required: list[str] = []
        normalized_content = self._normalize_facility_text(content)
        for name, keywords in FACILITY_KEYWORDS.items():
            normalized_keywords = [self._normalize_facility_text(keyword) for keyword in keywords]
            if any(keyword in normalized_content for keyword in normalized_keywords):
                required.append(name)
        if not required:
            return None
        required = list(dict.fromkeys(required))
        match_mode = DEFAULT_FACILITY_MODE
        min_match_count: int | None = len(required)
        exact_count_match = re.search(r"(?:任意|至少)(\d+)个(?:即可|就行|满足)?", content)
        chinese_count_match = re.search(r"(?:任意|至少)(一个|两个|二个|三个)(?:即可|就行|满足)?", content)
        if exact_count_match:
            match_mode = "at_least"
            min_match_count = int(exact_count_match.group(1))
        elif chinese_count_match:
            mapping = {"一个": 1, "两个": 2, "二个": 2, "三个": 3}
            match_mode = "at_least"
            min_match_count = mapping.get(chinese_count_match.group(1), len(required))
        elif re.search(r"任意一个|任一|任意即可", content):
            match_mode = "any"
            min_match_count = 1
        elif re.search(r"全部满足|都满足|都要有|同时有|而且有|并且有|和", content):
            match_mode = "all"
            min_match_count = len(required)
        elif len(required) == 1:
            match_mode = "all"
            min_match_count = 1
        min_match_count = max(1, min(min_match_count or 1, len(required)))
        return {"required": required, "matchMode": match_mode, "minMatchCount": min_match_count}

    def _extract_room_type(self, content: str) -> dict[str, Any] | None:
        room_type_id_match = re.search(r"(?:roomTypeId|房型ID|房型编号)\s*[:：]?\s*(\d+)", content, re.IGNORECASE)
        result: dict[str, Any] = {}
        if room_type_id_match:
            result["roomTypeId"] = int(room_type_id_match.group(1))
        for pattern in (
            r"(?:有|带|含|住|订|要|找|查|看)([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)",
            r"(?:房型|房间类型|房间|客房)(?:是|为|叫|有|选|要)?([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)",
            r"([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)(?:的?(?:酒店|宾馆|民宿|客栈|住宿|房型)|即可|就行|吗|呢|呀|啊|$)",
        ):
            room_type_match = re.search(pattern, content)
            if not room_type_match:
                continue
            candidate = self._normalize_room_type_candidate(room_type_match.group(1))
            if self._is_valid_room_type_candidate(candidate):
                result["roomTypeKeyword"] = candidate
                break
        return result or None

    @staticmethod
    def _extract_price_range(content: str) -> tuple[float | None, float | None]:
        range_match = re.search(r"(\d+(?:\.\d+)?)\s*[-到至]\s*(\d+(?:\.\d+)?)\s*元?", content)
        if range_match:
            return float(range_match.group(1)), float(range_match.group(2))
        min_price = None
        max_price = None
        for pattern in (
            r"(\d+(?:\.\d+)?)\s*元?(?:以上|起|至少|起价)",
            r"(?:不少于|不低于|至少|高于|大于)(\d+(?:\.\d+)?)\s*元?",
        ):
            min_match = re.search(pattern, content)
            if min_match:
                min_price = float(min_match.group(1))
                break
        for pattern in (
            r"(\d+(?:\.\d+)?)\s*元?(?:以下|以内)",
            r"(?:不超过|不高于|最多|至多|低于|小于)(\d+(?:\.\d+)?)\s*元?",
            r"(?:价格|价位|预算)(?:不超过|不高于|低于|小于|最多|至多|在)?(\d+(?:\.\d+)?)\s*元?",
        ):
            max_match = re.search(pattern, content)
            if max_match:
                max_price = float(max_match.group(1))
                break
        return min_price, max_price

    @staticmethod
    def _extract_rating(content: str) -> float | None:
        rating_match = re.search(r"([1-5](?:\.\d)?)\s*(?:分|星)(?:以上|及以上)?", content)
        return float(rating_match.group(1)) if rating_match else None

    @staticmethod
    def _extract_explicit_order_id(content: str) -> int | None:
        match = re.search(r"(?:订单号?|orderId|reservationId)[:：#]?\s*(\d{1,18})", content, re.IGNORECASE)
        return int(match.group(1)) if match else None

    @staticmethod
    def _ordinal_token_to_index(token: str) -> int | None:
        if not token:
            return None
        if token.isdigit():
            return max(int(token) - 1, 0)
        mapping = {"一": 1, "二": 2, "两": 2, "三": 3, "四": 4, "五": 5, "六": 6, "七": 7, "八": 8, "九": 9, "十": 10}
        if token in mapping:
            return mapping[token] - 1
        if token.startswith("十") and token[1:] in mapping:
            return 10 + mapping[token[1:]] - 1
        if token.endswith("十") and token[:-1] in mapping:
            return mapping[token[:-1]] * 10 - 1
        return None

    def _extract_order_candidate_index(self, content: str) -> int | None:
        match = re.search(r"第([一二两三四五六七八九十\d]+)条", content)
        return self._ordinal_token_to_index(match.group(1)) if match else None

    def _resolve_candidate_order_query(self, content: str, guest_id: int, order_candidates: list[dict[str, Any]]) -> dict[str, Any] | None:
        if not order_candidates:
            return None
        explicit_order_id = self._extract_explicit_order_id(content)
        if explicit_order_id is not None:
            return {"guestId": guest_id, "reservationId": explicit_order_id, "limit": 1}
        candidate_index = self._extract_order_candidate_index(content)
        if candidate_index is None and any(token in content for token in ("这条订单", "该订单", "这笔订单", "上一条", "上一个", "最近预订的订单", "最近订单")):
            candidate_index = 0
        if candidate_index is None or candidate_index < 0 or candidate_index >= len(order_candidates):
            return None
        target = order_candidates[candidate_index] or {}
        target_id = target.get("reservationId") or target.get("orderId")
        return {"guestId": guest_id, "reservationId": int(target_id), "limit": 1} if target_id is not None else None

    @staticmethod
    def _extract_source_order_room_type(content: str) -> str | None:
        for pattern in (
            r"的([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)(?:改为|改成|换成|换为|调整为|改到|换到|变更为|为)",
            r"把([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)(?:改为|改成|换成|换为|调整为|改到|换到|变更为)",
            r"由([\u4e00-\u9fa5A-Za-z0-9]{1,12}房)(?:改为|改成|换成|换为|调整为|改到|换到|变更为)",
        ):
            match = re.search(pattern, content)
            if match:
                return match.group(1)
        return None

    @staticmethod
    def _extract_cancel_reason(message: str) -> str | None:
        return "用户主动取消" if "不要了" in message else None

    def _detect_order_action(self, message: str) -> str | None:
        content = self._normalize_message(message)
        if not self._looks_like_order_request(content):
            return None
        if any(keyword in content for keyword in ORDER_CANCEL_HINTS):
            return ORDER_CANCEL_INTENT
        if any(keyword in content for keyword in ORDER_UPDATE_HINTS):
            return ORDER_UPDATE_INTENT
        return ORDER_QUERY_INTENT

    def _looks_like_order_request(self, message: str) -> bool:
        content = self._normalize_message(message)
        if not content:
            return False
        has_order_signal = any(keyword in content for keyword in ORDER_QUERY_HINTS) or content.startswith("帮我查最近订单")
        if not has_order_signal:
            return False
        if self._looks_like_hotel_search(content) and "订单" not in content and "预订" not in content and not any(keyword in content for keyword in ORDER_CANCEL_HINTS + ORDER_UPDATE_HINTS):
            return False
        return True

    def _looks_like_knowledge_query(self, message: str) -> bool:
        content = self._normalize_message(message)
        if not content:
            return False
        if any(keyword in content for keyword in KNOWLEDGE_QUERY_HINTS):
            return True
        if (
            any(token in (message or "") for token in ("？", "?"))
            and any(keyword in content for keyword in KNOWLEDGE_DOMAIN_HINTS)
            and not self._looks_like_order_request(content)
            and not self._looks_like_hotel_search(content)
        ):
            return True
        return False

    @staticmethod
    def _extract_recent_days(content: str) -> int | None:
        days_match = re.search(r"最近(\d+)天", content)
        if days_match:
            return int(days_match.group(1))
        if "最近一周" in content or "最近7天" in content:
            return 7
        if "最近一个月" in content or "最近1个月" in content or "最近30天" in content:
            return 30
        return None

    def _extract_order_limit(self, content: str) -> int | None:
        for pattern in (
            r"(?:最近|最新|前|近)([一二两三四五六七八九十百\d]+)条订单",
            r"([一二两三四五六七八九十百\d]+)条订单",
        ):
            match = re.search(pattern, content)
            if not match:
                continue
            limit = self._count_token_to_int(match.group(1))
            if limit is not None:
                return max(1, min(limit, 20))
        return None

    @staticmethod
    def _count_token_to_int(token: str) -> int | None:
        cleaned = (token or "").strip()
        if not cleaned:
            return None
        if cleaned.isdigit():
            return int(cleaned)
        mapping = {"一": 1, "二": 2, "两": 2, "三": 3, "四": 4, "五": 5, "六": 6, "七": 7, "八": 8, "九": 9}
        if cleaned == "十":
            return 10
        if cleaned in mapping:
            return mapping[cleaned]
        if cleaned.startswith("十") and cleaned[1:] in mapping:
            return 10 + mapping[cleaned[1:]]
        if cleaned.endswith("十") and cleaned[:-1] in mapping:
            return mapping[cleaned[:-1]] * 10
        if "十" in cleaned:
            left, right = cleaned.split("十", 1)
            left_value = mapping.get(left, 1 if left == "" else None)
            right_value = mapping.get(right, 0 if right == "" else None)
            if left_value is not None and right_value is not None:
                return left_value * 10 + right_value
        return None

    @staticmethod
    def _extract_order_status(content: str) -> str | None:
        if any(keyword in content for keyword in ("已取消", "取消的订单")):
            return "cancelled"
        if any(keyword in content for keyword in ("已完成", "完成的订单")):
            return "completed"
        if any(keyword in content for keyword in ("已入住", "入住中的订单")):
            return "checked_in"
        return None

    @staticmethod
    def _normalize_room_type_candidate(candidate: str) -> str:
        normalized = (candidate or "").strip().strip("的")
        changed = True
        while changed and normalized:
            changed = False
            for prefix in ROOM_TYPE_PREFIXES:
                if normalized.startswith(prefix) and len(normalized) > len(prefix):
                    normalized = normalized[len(prefix):].strip().strip("的")
                    changed = True
        return normalized

    @staticmethod
    def _is_valid_room_type_candidate(candidate: str) -> bool:
        cleaned = (candidate or "").strip()
        if not cleaned or cleaned == ROOM_TYPE_SUFFIX:
            return False
        normalized = cleaned.lower()
        if "酒店" in cleaned or "宾馆" in cleaned:
            return False
        if any(keyword in normalized for keyword in ("wifi", "wi-fi", "早餐", "停车", "价格", "预算", "订单", "预订", "改为", "改成", "换为", "换成", "调整", "变更")):
            return False
        return cleaned.endswith(ROOM_TYPE_SUFFIX)
    def _looks_like_hotel_search(self, message: str) -> bool:
        content = self._normalize_message(message).lower()
        if any(keyword in content for keyword in HOTEL_SEARCH_HINTS):
            return True
        return any(keyword.lower() in content for keyword in HOTEL_CHAIN_KEYWORDS)

    @staticmethod
    def _looks_like_city(candidate: str) -> bool:
        return candidate.endswith(CITY_SUFFIXES)

    @staticmethod
    def _looks_like_district(candidate: str) -> bool:
        return candidate.endswith(DISTRICT_SUFFIXES)

    @staticmethod
    def _looks_like_street(candidate: str) -> bool:
        return candidate.endswith(("路", "街", "道", "巷", "里", "镇"))

    def _looks_like_street_or_landmark(self, candidate: str) -> bool:
        return candidate.endswith(LANDMARK_SUFFIXES)

    @staticmethod
    def _normalize_location_token(token: str) -> str:
        cleaned = (token or "").strip()
        cleaned = re.sub(r"市市$", "市", cleaned)
        cleaned = re.sub(r"区区$", "区", cleaned)
        cleaned = re.sub(r"县县$", "县", cleaned)
        cleaned = re.sub(r"路路$", "路", cleaned)
        cleaned = re.sub(r"^(?:请把|把|请将|将)?(?:我想在|我要在|我在|帮我在|替我在|给我在|在)", "", cleaned)
        return cleaned

    def _preprocess_location_content(self, content: str) -> str:
        cleaned = self._strip_request_prefix(content)
        return re.sub(r"^(?:请把|把|请将|将)?(?:我想在|我要在|我在|帮我在|替我在|给我在|在)", "", cleaned)

    @staticmethod
    def _looks_like_loose_location_candidate(candidate: str) -> bool:
        cleaned = (candidate or "").strip()
        if not 2 <= len(cleaned) <= 12:
            return False
        if re.search(r"\d", cleaned):
            return False
        if cleaned in QUERY_CONNECTOR_WORDS or all(char in "".join(QUERY_CONNECTOR_WORDS) for char in cleaned):
            return False
        return not any(word in cleaned for word in LOCATION_NOISE_WORDS)

    @staticmethod
    def _strip_connector_words(text: str) -> str:
        cleaned = text or ""
        for token in QUERY_CONNECTOR_WORDS:
            cleaned = cleaned.replace(token, "")
        return cleaned

    @staticmethod
    def _normalize_message(message: str) -> str:
        return re.sub(r"\s+", "", message or "")

    @staticmethod
    def _strip_request_prefix(content: str) -> str:
        return re.sub(
            r"^(?:(?:请|麻烦你|麻烦|帮我|帮忙|给我|替我|我想|我要)+)?(?:(?:查一下|查一查|查询|查找|搜索|搜一下|搜一搜|看一下|看看|推荐)+)?",
            "",
            content or "",
        )

    @staticmethod
    def _normalize_date_token(date_text: str) -> str:
        normalized = date_text.replace("年", "-").replace("月", "-").replace("日", "").replace("/", "-")
        year, month, day = normalized.split("-")
        return f"{int(year):04d}-{int(month):02d}-{int(day):02d}"

    @staticmethod
    def _has_valid_hotel_condition(query: dict[str, Any]) -> bool:
        if not query:
            return False
        location = query.get("location") or {}
        facilities = query.get("facilities") or {}
        room_type = query.get("roomType") or {}
        return bool(location) or bool(facilities.get("required")) or bool(room_type) or any(query.get(key) not in (None, "", []) for key in ("hotelName", "minPrice", "maxPrice", "rating"))

    @staticmethod
    def _compact_query_object(source: Any) -> Any:
        if isinstance(source, dict):
            compacted = {key: ChatService._compact_query_object(value) for key, value in source.items()}
            return {key: value for key, value in compacted.items() if value not in (None, "", [], {})}
        if isinstance(source, list):
            compacted_list = [ChatService._compact_query_object(item) for item in source]
            return [item for item in compacted_list if item not in (None, "", [], {})]
        return source

    @staticmethod
    def _has_meaningful_order_payload(order: Any) -> bool:
        if not isinstance(order, dict):
            return False
        return any(order.get(key) not in (None, "", [], {}) for key in ("reservationId", "orderId", "orderNo", "hotelName", "roomTypeName", "checkInDate", "checkOutDate", "status", "statusDescription"))

    def _llm_route_fallback(self, request: ChatRequest, hotel_query: dict[str, Any], order_action: str | None) -> dict[str, Any]:
        decision = self._default_route_decision()
        try:
            response = self._get_llm_client().chat.completions.create(
                model=self.settings.llm_model,
                messages=[
                    {"role": "system", "content": ROUTER_PROMPT},
                    {
                        "role": "user",
                        "content": json.dumps(
                            {
                                "message": request.message,
                                "hotel_query": hotel_query,
                                "order_action": order_action,
                                "looks_like_hotel_search": self._looks_like_hotel_search(request.message),
                                "looks_like_knowledge_query": self._looks_like_knowledge_query(request.message),
                            },
                            ensure_ascii=False,
                        ),
                    },
                ],
            )
            raw = (response.choices[0].message.content or "").strip()
            parsed = json.loads(raw)
            if isinstance(parsed, dict):
                decision.update(parsed)
        except Exception as exc:
            decision["reason"] = f"llm_route_fallback_failed: {exc}"
        return decision

    @staticmethod
    def _empty_hotel_structured_data() -> dict[str, Any]:
        return {"query": {}, "total": 0, "hotels": []}
