#调用Spring Boot接口
import os
import requests
from dotenv import load_dotenv

load_dotenv()

BIZ_BACKEND_BASE_URL = os.getenv("BIZ_BACKEND_BASE_URL", "http://localhost:8080")
BIZ_BACKEND_TOKEN = os.getenv("BIZ_BACKEND_TOKEN", "")


def _headers():
    headers = {}
    if BIZ_BACKEND_TOKEN:
        headers["X-Internal-Token"] = BIZ_BACKEND_TOKEN
    return headers


def get_recent_orders(user_id: str):
    """查询当前用户最近订单"""
    resp = requests.get(
        f"{BIZ_BACKEND_BASE_URL}/agent/orders/recent",
        params={"userId": user_id},
        headers=_headers(),
        timeout=10,
    )
    resp.raise_for_status()
    return resp.json()


def get_order_detail(order_id: str, user_id: str):
    """查询订单详情"""
    resp = requests.get(
        f"{BIZ_BACKEND_BASE_URL}/agent/orders/{order_id}",
        params={"userId": user_id},
        headers=_headers(),
        timeout=10,
    )
    resp.raise_for_status()
    return resp.json()


def get_hotel_info(hotel_id: str):
    """查询酒店基础信息"""
    resp = requests.get(
        f"{BIZ_BACKEND_BASE_URL}/agent/hotels/{hotel_id}",
        headers=_headers(),
        timeout=10,
    )
    resp.raise_for_status()
    return resp.json()

#定义给模型看的工具描述
TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "get_recent_orders",
            "strict": True,
            "description": "获取当前用户最近订单，用于查询最近订单状态、入住时间、金额等信息",
            "parameters": {
                "type": "object",
                "properties": {
                    "user_id": {
                        "type": "string",
                        "description": "当前登录用户ID"
                    }
                },
                "required": ["user_id"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "get_order_detail",
            "description": "根据订单ID查询订单详情",
            "parameters": {
                "type": "object",
                "properties": {
                    "order_id": {
                        "type": "string",
                        "description": "订单ID"
                    },
                    "user_id": {
                        "type": "string",
                        "description": "当前登录用户ID"
                    }
                },
                "required": ["order_id", "user_id"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "get_hotel_info",
            "description": "根据酒店ID查询酒店基础信息",
            "parameters": {
                "type": "object",
                "properties": {
                    "hotel_id": {
                        "type": "string",
                        "description": "酒店ID"
                    }
                },
                "required": ["hotel_id"]
            }
        }
    }
]

#建立工具名到Python函数的映射
TOOL_FUNC_MAP = {
    "get_recent_orders": get_recent_orders,
    "get_order_detail": get_order_detail,
    "get_hotel_info": get_hotel_info,
}