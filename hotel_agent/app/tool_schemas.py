TOOL_SCHEMAS = [
    {
        "type": "function",
        "function": {
            "name": "search_hotels",
            "strict": True,
            "description": "Search hotels with structured location, facility, and room-type filters. Different categories combine with AND, and facility matching supports all, any, or at_least.",
            "parameters": {
                "type": "object",
                "properties": {
                    "location": {
                        "type": ["object", "null"],
                        "properties": {
                            "province": {"type": ["string", "null"]},
                            "city": {"type": ["string", "null"]},
                            "district": {"type": ["string", "null"]},
                            "districtKeyword": {"type": ["string", "null"]},
                            "street": {"type": ["string", "null"]},
                            "addressKeyword": {"type": ["string", "null"]}
                        },
                        "required": [],
                        "additionalProperties": False
                    },
                    "facilities": {
                        "type": ["object", "null"],
                        "properties": {
                            "required": {
                                "type": ["array", "null"],
                                "items": {"type": "string"}
                            },
                            "matchMode": {"type": ["string", "null"]},
                            "minMatchCount": {"type": ["integer", "null"]}
                        },
                        "required": [],
                        "additionalProperties": False
                    },
                    "roomType": {
                        "type": ["object", "null"],
                        "properties": {
                            "roomTypeId": {"type": ["integer", "null"]},
                            "roomTypeKeyword": {"type": ["string", "null"]}
                        },
                        "required": [],
                        "additionalProperties": False
                    },
                    "hotelName": {"type": ["string", "null"]},
                    "minPrice": {"type": ["number", "null"]},
                    "maxPrice": {"type": ["number", "null"]},
                    "rating": {"type": ["number", "null"]},
                    "checkInDate": {"type": ["string", "null"]},
                    "checkOutDate": {"type": ["string", "null"]},
                    "roomCount": {"type": ["integer", "null"]}
                },
                "required": [],
                "additionalProperties": False
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "search_orders",
            "strict": True,
            "description": "Search the current guest's orders with structured filters like recentDays, city, district, hotelName, status, limit, and sort.",
            "parameters": {
                "type": "object",
                "properties": {
                    "filters": {
                        "type": ["object", "null"],
                        "properties": {
                            "guestId": {"type": ["integer", "null"]},
                            "recentDays": {"type": ["integer", "null"]},
                            "province": {"type": ["string", "null"]},
                            "city": {"type": ["string", "null"]},
                            "district": {"type": ["string", "null"]},
                            "hotelName": {"type": ["string", "null"]},
                            "status": {"type": ["string", "null"]},
                            "limit": {"type": ["integer", "null"]},
                            "sort": {"type": ["string", "null"]}
                        },
                        "required": [],
                        "additionalProperties": False
                    },
                    "guest_id": {"type": ["integer", "null"], "description": "Current guest id, injected by the chat service when available."}
                },
                "required": [],
                "additionalProperties": False
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "get_recent_orders",
            "strict": True,
            "description": "Get the current guest's recent orders.",
            "parameters": {
                "type": "object",
                "properties": {
                    "guest_id": {"type": "integer", "description": "Current guest id."},
                    "limit": {"type": ["integer", "null"], "description": "Maximum number of records to return."}
                },
                "required": ["guest_id"],
                "additionalProperties": False
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "get_order_detail",
            "strict": True,
            "description": "Get the details of a specific order by reservation id.",
            "parameters": {
                "type": "object",
                "properties": {
                    "reservation_id": {"type": "integer", "description": "Reservation id."},
                    "guest_id": {"type": "integer", "description": "Current guest id."}
                },
                "required": ["reservation_id", "guest_id"],
                "additionalProperties": False
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "update_order",
            "strict": True,
            "description": "Update order dates or room type and return the updated order.",
            "parameters": {
                "type": "object",
                "properties": {
                    "reservation_id": {"type": "integer", "description": "Reservation id."},
                    "guest_id": {"type": "integer", "description": "Current guest id."},
                    "check_in_date": {"type": ["string", "null"], "description": "New check-in date in yyyy-MM-dd format."},
                    "check_out_date": {"type": ["string", "null"], "description": "New check-out date in yyyy-MM-dd format."},
                    "room_type_id": {"type": ["integer", "null"], "description": "New room type id."},
                    "room_type_keyword": {"type": ["string", "null"], "description": "Fuzzy room type keyword, such as ??? or ???."}
                },
                "required": ["reservation_id", "guest_id"],
                "additionalProperties": False
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "update_order_by_query",
            "strict": True,
            "description": "Search orders by natural-language-derived filters, update the unique matched order, or return candidate orders if multiple orders match.",
            "parameters": {
                "type": "object",
                "properties": {
                    "filters": {
                        "type": ["object", "null"],
                        "properties": {
                            "guestId": {"type": ["integer", "null"]},
                            "recentDays": {"type": ["integer", "null"]},
                            "province": {"type": ["string", "null"]},
                            "city": {"type": ["string", "null"]},
                            "district": {"type": ["string", "null"]},
                            "hotelName": {"type": ["string", "null"]},
                            "status": {"type": ["string", "null"]},
                            "limit": {"type": ["integer", "null"]},
                            "sort": {"type": ["string", "null"]}
                        },
                        "required": [],
                        "additionalProperties": False
                    },
                    "updates": {
                        "type": ["object", "null"],
                        "properties": {
                            "checkInDate": {"type": ["string", "null"]},
                            "checkOutDate": {"type": ["string", "null"]},
                            "roomTypeId": {"type": ["integer", "null"]},
                            "roomTypeKeyword": {"type": ["string", "null"]}
                        },
                        "required": [],
                        "additionalProperties": False
                    },
                    "guest_id": {"type": ["integer", "null"], "description": "Current guest id, injected by the chat service when available."}
                },
                "required": [],
                "additionalProperties": False
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "cancel_order",
            "strict": True,
            "description": "Cancel an order and return the latest order status.",
            "parameters": {
                "type": "object",
                "properties": {
                    "reservation_id": {"type": "integer", "description": "Reservation id."},
                    "guest_id": {"type": "integer", "description": "Current guest id."},
                    "reason": {"type": ["string", "null"], "description": "Cancellation reason."}
                },
                "required": ["reservation_id", "guest_id"],
                "additionalProperties": False
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "cancel_order_by_query",
            "strict": True,
            "description": "Search orders by natural-language-derived filters, cancel the unique matched order, or return candidate orders if multiple orders match.",
            "parameters": {
                "type": "object",
                "properties": {
                    "filters": {
                        "type": ["object", "null"],
                        "properties": {
                            "guestId": {"type": ["integer", "null"]},
                            "recentDays": {"type": ["integer", "null"]},
                            "province": {"type": ["string", "null"]},
                            "city": {"type": ["string", "null"]},
                            "district": {"type": ["string", "null"]},
                            "hotelName": {"type": ["string", "null"]},
                            "status": {"type": ["string", "null"]},
                            "limit": {"type": ["integer", "null"]},
                            "sort": {"type": ["string", "null"]}
                        },
                        "required": [],
                        "additionalProperties": False
                    },
                    "reason": {"type": ["string", "null"]},
                    "guest_id": {"type": ["integer", "null"], "description": "Current guest id, injected by the chat service when available."}
                },
                "required": [],
                "additionalProperties": False
            }
        }
    }
]

