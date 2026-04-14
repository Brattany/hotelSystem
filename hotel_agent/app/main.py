import os
import json
from dotenv import load_dotenv
from fastapi import FastAPI, Body
from openai import OpenAI
from tools import TOOLS, TOOL_FUNC_MAP

load_dotenv()

app = FastAPI()

client = OpenAI(
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
)

MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-chat")


@app.get("/health")
def health():
    return {"ok": True}


@app.post("/chat")
def chat(data: dict = Body(...)):
    user_id = data.get("user_id", "u1001")
    user_message = data.get("message", "")

    messages = [
        {
            "role": "system",
            "content": (
                "你是酒店智能客服。"
                "当问题涉及订单、酒店信息、订单状态、入住时间等精确信息时，优先调用工具，不要猜测。"
            )
        },
        {
            "role": "user",
            "content": user_message
        }
    ]

    try:
        # 第一次调用：让模型决定是否调用工具
        response = client.chat.completions.create(
            model=MODEL,
            messages=messages,
            tools=TOOLS,
            tool_choice="auto"
        )

        assistant_message = response.choices[0].message

        # 如果模型没有调用工具，直接返回普通回答
        if not getattr(assistant_message, "tool_calls", None):
            return {
                "answer": assistant_message.content,
                "used_tools": []
            }

        used_tools = []

        # 先把模型这条“我要调用工具”的消息加入上下文
        messages.append(assistant_message)

        # 执行模型要求的每个工具
        for tool_call in assistant_message.tool_calls:
            tool_name = tool_call.function.name
            tool_args = json.loads(tool_call.function.arguments)

            # 自动补充当前 user_id
            if "user_id" in tool_args and not tool_args["user_id"]:
                tool_args["user_id"] = user_id
            elif "user_id" not in tool_args and tool_name in ["get_recent_orders", "get_order_detail"]:
                tool_args["user_id"] = user_id

            if tool_name not in TOOL_FUNC_MAP:
                tool_result = {"error": f"未找到工具: {tool_name}"}
            else:
                try:
                    tool_result = TOOL_FUNC_MAP[tool_name](**tool_args)
                except Exception as e:
                    tool_result = {"error": str(e)}

            used_tools.append({
                "tool_name": tool_name,
                "tool_args": tool_args,
                "tool_result": tool_result
            })

            # 把工具执行结果回传给模型
            messages.append({
                "role": "tool",
                "tool_call_id": tool_call.id,
                "content": json.dumps(tool_result, ensure_ascii=False)
            })

        # 第二次调用：让模型基于工具结果生成最终自然语言回答
        final_response = client.chat.completions.create(
            model=MODEL,
            messages=messages
        )

        final_answer = final_response.choices[0].message.content

        return {
            "answer": final_answer,
            "used_tools": used_tools
        }

    except Exception as e:
        return {
            "error_type": type(e).__name__,
            "error_message": str(e)
        }