# -*- coding: utf-8 -*-
# 新工具实现统一从 tool_runtime 导出，保留当前文件作为兼容入口。
from .tool_runtime import (  
    TOOLS,
    TOOL_FUNC_MAP,
    cancel_order,
    cancel_order_by_query,
    get_order_detail,
    get_recent_orders,
    search_orders,
    search_hotels,
    update_order,
    update_order_by_query,
)

__all__ = [
    "TOOLS",
    "TOOL_FUNC_MAP",
    "search_hotels",
    "search_orders",
    "get_recent_orders",
    "get_order_detail",
    "update_order",
    "update_order_by_query",
    "cancel_order",
    "cancel_order_by_query",
]
