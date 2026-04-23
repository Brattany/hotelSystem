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
    ORDER_UPDATE_INTENT,
    ROUTE_STRUCTURED,
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


if __name__ == "__main__":
    unittest.main()
