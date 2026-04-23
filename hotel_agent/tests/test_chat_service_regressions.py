import sys
import types
import unittest


fake_openai = types.ModuleType("openai")


class OpenAI:
    def __init__(self, *args, **kwargs):
        pass


fake_openai.OpenAI = OpenAI
sys.modules.setdefault("openai", fake_openai)

from hotelSystem.hotel_agent.app.chat_service import (  # noqa: E402
    ChatService,
    ChatResponse,
    ORDER_UPDATE_INTENT,
    ROUTE_STRUCTURED,
    ROUTE_HYBRID,
)


class _Request:
    def __init__(self, message: str) -> None:
        self.message = message
        self.route_hint = ""
        self.top_k = None


class ChatServiceRegressionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.service = ChatService()

    def test_price_filter_extracts_max_price_without_fake_city(self) -> None:
        query = self.service._extract_hotel_query("查询价格不超过300元的酒店")

        self.assertEqual(query.get("maxPrice"), 300.0)
        self.assertFalse(query.get("hotelName"))
        self.assertFalse((query.get("location") or {}).get("city"))

    def test_room_type_filter_extracts_city_and_room_type(self) -> None:
        query = self.service._extract_hotel_query("查询成都市有大床房的酒店")

        self.assertEqual((query.get("location") or {}).get("city"), "成都市")
        self.assertEqual((query.get("roomType") or {}).get("roomTypeKeyword"), "大床房")

    def test_facility_filter_stays_structured_instead_of_breakfast_knowledge(self) -> None:
        query = self.service._extract_hotel_query("查询成都市有早餐和停车场的酒店")
        decision = self.service._decide_route(_Request("查询成都市有早餐和停车场的酒店"))

        self.assertEqual((query.get("facilities") or {}).get("required"), ["breakfast", "parking"])
        self.assertEqual(decision.get("route_type"), ROUTE_STRUCTURED)
        self.assertFalse(decision.get("knowledge_query"))

    def test_wifi_breakfast_query_does_not_generate_fake_location_echo(self) -> None:
        query = self.service._extract_hotel_query("查询有wifi和早餐的酒店")
        reply = self.service._build_hotel_reply(
            {
                "query": query,
                "total": 0,
                "hotels": [],
            }
        )

        self.assertEqual((query.get("facilities") or {}).get("required"), ["wifi", "breakfast"])
        self.assertFalse((query.get("location") or {}).get("city"))
        self.assertIn("WiFi、早餐", reply)
        self.assertNotIn("有和", reply)

    def test_wifi_breakfast_synonyms_are_normalized(self) -> None:
        query = self.service._extract_hotel_query("有无线网络和含早的酒店")

        self.assertEqual((query.get("facilities") or {}).get("required"), ["wifi", "breakfast"])
        self.assertFalse((query.get("location") or {}).get("city"))

    def test_order_update_extracts_normalized_location_and_no_empty_order_payload_on_failure(self) -> None:
        message = "将我在贵阳市观山湖区如家酒店的订单预订房型改为海景房"
        action = self.service._detect_order_action(message)
        order_query = self.service._extract_order_query(message, 123, action or ORDER_UPDATE_INTENT, [])
        order_updates = self.service._extract_order_updates(message)
        structured_data = self.service._build_structured_data(
            [
                {
                    "tool": "update_order_by_query",
                    "ok": False,
                    "error": {"message": "Target room type does not exist."},
                    "query": order_query,
                    "matched": True,
                    "multiple": False,
                    "candidates": [],
                    "total": 1,
                    "order": None,
                }
            ]
        )

        self.assertEqual(action, ORDER_UPDATE_INTENT)
        self.assertEqual(order_query.get("city"), "贵阳市")
        self.assertEqual(order_query.get("district"), "观山湖区")
        self.assertEqual(order_query.get("hotelName"), "如家酒店")
        self.assertEqual(order_updates.get("roomTypeKeyword"), "海景房")
        self.assertNotIn("order", structured_data)
        self.assertNotIn("reservation", structured_data)

    def test_order_update_date_query_does_not_pollute_city_or_price(self) -> None:
        message = "修改我在贵阳市观山湖区如家酒店的订单预订入住时间为2026-05-01"
        action = self.service._detect_order_action(message)
        hotel_query = self.service._extract_hotel_query(message)
        order_query = self.service._extract_order_query(message, 123, action or ORDER_UPDATE_INTENT, [])
        order_updates = self.service._extract_order_updates(message)

        self.assertEqual(action, ORDER_UPDATE_INTENT)
        self.assertEqual(order_query.get("city"), "贵阳市")
        self.assertEqual(order_query.get("district"), "观山湖区")
        self.assertEqual(order_query.get("hotelName"), "如家酒店")
        self.assertEqual(order_updates.get("checkInDate"), "2026-05-01")
        self.assertFalse(hotel_query.get("minPrice"))
        self.assertFalse(hotel_query.get("maxPrice"))

    def test_cancel_order_query_does_not_include_action_prefix_in_city(self) -> None:
        message = "取消我在贵阳市观山湖区如家酒店的订单"
        action = self.service._detect_order_action(message)
        order_query = self.service._extract_order_query(message, 123, action or "cancel_order", [])

        self.assertEqual(action, "cancel_order")
        self.assertEqual(order_query.get("city"), "贵阳市")
        self.assertEqual(order_query.get("district"), "观山湖区")
        self.assertEqual(order_query.get("hotelName"), "如家酒店")

    def test_hybrid_query_keeps_structured_context_and_knowledge_context(self) -> None:
        message = "查询贵阳市观山湖区如家酒店的退订规则"
        hotel_query = self.service._extract_hotel_query(message)
        decision = self.service._decide_route(_Request(message))
        merged = self.service._merge_hybrid_response(
            ChatResponse(
                intent="search_hotels",
                structured_data={"query": hotel_query, "total": 0, "hotels": []},
                reply="暂时没有查到符合“贵阳市观山湖区 / 如家酒店”的酒店。",
                success=True,
                error=None,
                used_tools=[],
            ),
            ChatResponse(
                intent="knowledge_query",
                structured_data={
                    "knowledge": {
                        "total": 1,
                        "hits": [{"content": "支持按规则退订", "source": "sample"}],
                        "sources": [{"source": "sample"}],
                    },
                    "sources": [{"source": "sample"}],
                },
                reply="该酒店支持按规则退订。",
                success=True,
                error=None,
                used_tools=[],
            ),
            decision.get("knowledge_query") or "",
        )

        self.assertEqual(decision.get("route_type"), ROUTE_HYBRID)
        self.assertEqual(decision.get("knowledge_query"), "贵阳市观山湖区如家酒店取消政策")
        self.assertEqual((hotel_query.get("location") or {}).get("city"), "贵阳市")
        self.assertEqual((hotel_query.get("location") or {}).get("district"), "观山湖区")
        self.assertEqual(hotel_query.get("hotelName"), "如家酒店")
        self.assertIn("贵阳市观山湖区 / 如家酒店", merged.reply)
        self.assertIn("该酒店支持按规则退订", merged.reply)
        self.assertIn("知识补充", merged.reply)

    def test_order_update_other_city_and_room_type_is_generic(self) -> None:
        message = "将我在深圳市南山区海景公寓的订单预订房型改为双床房"
        action = self.service._detect_order_action(message)
        order_query = self.service._extract_order_query(message, 456, action or ORDER_UPDATE_INTENT, [])
        order_updates = self.service._extract_order_updates(message)

        self.assertEqual(action, ORDER_UPDATE_INTENT)
        self.assertEqual(order_query.get("city"), "深圳市")
        self.assertEqual(order_query.get("district"), "南山区")
        self.assertEqual(order_query.get("hotelName"), "海景公寓")
        self.assertEqual(order_updates.get("roomTypeKeyword"), "双床房")

    def test_order_update_other_city_date_does_not_pollute_price(self) -> None:
        message = "修改我在厦门市思明区鹭岛客栈的订单入住时间为2026-06-18"
        action = self.service._detect_order_action(message)
        hotel_query = self.service._extract_hotel_query(message)
        order_query = self.service._extract_order_query(message, 456, action or ORDER_UPDATE_INTENT, [])
        order_updates = self.service._extract_order_updates(message)

        self.assertEqual(action, ORDER_UPDATE_INTENT)
        self.assertEqual(order_query.get("city"), "厦门市")
        self.assertEqual(order_query.get("district"), "思明区")
        self.assertEqual(order_query.get("hotelName"), "鹭岛客栈")
        self.assertEqual(order_updates.get("checkInDate"), "2026-06-18")
        self.assertFalse(hotel_query.get("minPrice"))
        self.assertFalse(hotel_query.get("maxPrice"))

    def test_cancel_order_supports_generic_hotel_without_hotel_suffix(self) -> None:
        message = "取消我在杭州市西湖区云栖的订单"
        action = self.service._detect_order_action(message)
        order_query = self.service._extract_order_query(message, 456, action or "cancel_order", [])

        self.assertEqual(action, "cancel_order")
        self.assertEqual(order_query.get("city"), "杭州市")
        self.assertEqual(order_query.get("district"), "西湖区")
        self.assertEqual(order_query.get("hotelName"), "云栖")

    def test_hybrid_query_other_city_keeps_contextual_rule_query(self) -> None:
        message = "查询苏州市姑苏区悦宿民宿的发票规则"
        hotel_query = self.service._extract_hotel_query(message)
        decision = self.service._decide_route(_Request(message))

        self.assertEqual(decision.get("route_type"), ROUTE_HYBRID)
        self.assertEqual((hotel_query.get("location") or {}).get("city"), "苏州市")
        self.assertEqual((hotel_query.get("location") or {}).get("district"), "姑苏区")
        self.assertEqual(hotel_query.get("hotelName"), "悦宿民宿")
        self.assertEqual(decision.get("knowledge_query"), "苏州市姑苏区悦宿民宿发票规则")


if __name__ == "__main__":
    unittest.main()
