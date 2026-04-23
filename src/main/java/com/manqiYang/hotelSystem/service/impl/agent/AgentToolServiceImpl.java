package com.manqiYang.hotelSystem.service.impl.agent;

import com.manqiYang.hotelSystem.dto.agent.AgentHotelFacilityQuery;
import com.manqiYang.hotelSystem.dto.agent.AgentHotelLocationQuery;
import com.manqiYang.hotelSystem.dto.agent.AgentHotelRoomTypeQuery;
import com.manqiYang.hotelSystem.dto.agent.AgentHotelSearchRequest;
import com.manqiYang.hotelSystem.dto.agent.AgentOrderCancelRequest;
import com.manqiYang.hotelSystem.dto.agent.AgentOrderSearchRequest;
import com.manqiYang.hotelSystem.dto.agent.AgentOrderUpdateRequest;
import com.manqiYang.hotelSystem.entity.reservation.Reservation;
import com.manqiYang.hotelSystem.entity.room.RoomType;
import com.manqiYang.hotelSystem.enums.reservation.ReservationEnum;
import com.manqiYang.hotelSystem.mapper.agent.AgentToolMapper;
import com.manqiYang.hotelSystem.mapper.reservation.ReservationMapper;
import com.manqiYang.hotelSystem.service.agent.AgentToolService;
import com.manqiYang.hotelSystem.service.room.RoomTypeService;
import com.manqiYang.hotelSystem.vo.agent.AgentHotelSearchVO;
import com.manqiYang.hotelSystem.vo.agent.AgentOrderDetailVO;
import com.manqiYang.hotelSystem.vo.agent.AgentOrderSummaryVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AgentToolServiceImpl implements AgentToolService {

    private static final Logger log = LoggerFactory.getLogger(AgentToolServiceImpl.class);

    @Autowired
    private AgentToolMapper agentToolMapper;

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private RoomTypeService roomTypeService;

    @Value("${agent.validation.enforce-order-ownership:false}")
    private boolean enforceOrderOwnership;

    @Value("${agent.validation.enforce-order-status:false}")
    private boolean enforceOrderStatus;

    @Override
    public List<AgentHotelSearchVO> searchHotels(AgentHotelSearchRequest request) {
        AgentHotelSearchRequest safeRequest = request == null ? new AgentHotelSearchRequest() : request;
        AgentHotelLocationQuery location = safeRequest.getLocation() == null ? new AgentHotelLocationQuery() : safeRequest.getLocation();
        AgentHotelFacilityQuery facilities = safeRequest.getFacilities() == null ? new AgentHotelFacilityQuery() : safeRequest.getFacilities();
        AgentHotelRoomTypeQuery roomType = safeRequest.getRoomType() == null ? new AgentHotelRoomTypeQuery() : safeRequest.getRoomType();

        BigDecimal minPrice = safeRequest.getMinPrice();
        BigDecimal maxPrice = safeRequest.getMaxPrice();
        BigDecimal rating = safeRequest.getRating();
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal swap = minPrice;
            minPrice = maxPrice;
            maxPrice = swap;
        }

        Integer roomCount = safeRequest.getRoomCount();
        if (roomCount != null && roomCount < 1) {
            throw new RuntimeException("Room count must be at least 1.");
        }

        String province = normalizeProvinceKeyword(location.getProvince());
        String city = normalizeCityKeyword(location.getCity());
        String district = normalizeDistrictKeyword(location.getDistrict());
        String districtKeyword = normalizeDistrictKeyword(location.getDistrictKeyword());
        String street = normalizeText(location.getStreet());
        String addressKeyword = normalizeText(location.getAddressKeyword());
        if (districtKeyword == null && district != null) {
            districtKeyword = district;
        }
        if (addressKeyword == null && street != null) {
            addressKeyword = street;
        }

        String hotelName = normalizeHotelKeyword(safeRequest.getHotelName());
        String roomTypeKeyword = normalizeRoomTypeKeyword(roomType.getRoomTypeKeyword());
        Long roomTypeId = roomType.getRoomTypeId();

        List<String> requiredFacilities = normalizeFacilities(facilities.getRequired());
        String facilityMatchMode = normalizeFacilityMatchMode(facilities.getMatchMode(), requiredFacilities.size());
        Integer facilityMinMatchCount = normalizeFacilityMinMatchCount(facilityMatchMode, facilities.getMinMatchCount(), requiredFacilities.size());
        Integer facilityCount = requiredFacilities.isEmpty() ? null : requiredFacilities.size();
        Integer requireWifi = requiredFacilities.contains("wifi") ? 1 : null;
        Integer requireBreakfast = requiredFacilities.contains("breakfast") ? 1 : null;
        Integer requireParking = requiredFacilities.contains("parking") ? 1 : null;

        LocalDate checkInDate = safeRequest.getCheckInDate();
        LocalDate checkOutDate = safeRequest.getCheckOutDate();
        if ((checkInDate == null) != (checkOutDate == null)) {
            checkInDate = null;
            checkOutDate = null;
        } else if (checkInDate != null && !checkOutDate.isAfter(checkInDate)) {
            throw new RuntimeException("Check-out date must be after check-in date.");
        }

        if (!hasAnyHotelSearchCondition(
                province,
                city,
                district,
                districtKeyword,
                street,
                addressKeyword,
                hotelName,
                minPrice,
                maxPrice,
                rating,
                roomTypeId,
                roomTypeKeyword,
                facilityCount
        )) {
            throw new RuntimeException("Please provide at least one valid hotel search condition.");
        }

        log.info(
                "agent_search_hotels normalizedFilters location={}/{}/{} districtKeyword={} street={} addressKeyword={} hotelName={} minPrice={} maxPrice={} roomTypeId={} roomTypeKeyword={} facilities={} matchMode={} roomCount={}",
                province,
                city,
                district,
                districtKeyword,
                street,
                addressKeyword,
                hotelName,
                minPrice,
                maxPrice,
                roomTypeId,
                roomTypeKeyword,
                requiredFacilities,
                facilityMatchMode,
                roomCount
        );

        List<AgentHotelSearchVO> hotels = agentToolMapper.searchHotels(
                province,
                city,
                district,
                districtKeyword,
                street,
                addressKeyword,
                hotelName,
                minPrice,
                maxPrice,
                rating,
                roomTypeId,
                roomTypeKeyword,
                facilityMatchMode,
                facilityMinMatchCount,
                facilityCount,
                requireWifi,
                requireBreakfast,
                requireParking,
                checkInDate,
                checkOutDate,
                roomCount
        );
        for (AgentHotelSearchVO hotel : hotels) {
            enrichMatchedRoomTypes(hotel);
        }
        log.info("agent_search_hotels_result hitCount={}", hotels.size());
        return hotels;
    }

    @Override
    public List<AgentOrderSummaryVO> getRecentOrders(Long guestId, Integer limit) {
        if (guestId == null || guestId <= 0) {
            throw new RuntimeException("guestId cannot be empty");
        }

        int safeLimit = limit == null ? 5 : Math.min(Math.max(limit, 1), 20);
        return agentToolMapper.selectRecentOrders(guestId, safeLimit);
    }

    @Override
    public List<AgentOrderSummaryVO> searchOrders(AgentOrderSearchRequest request) {
        AgentOrderSearchRequest safeRequest = request == null ? new AgentOrderSearchRequest() : request;
        Long guestId = safeRequest.getGuestId();
        if (guestId == null || guestId <= 0) {
            throw new RuntimeException("guestId cannot be empty");
        }

        Integer recentDays = safeRequest.getRecentDays();
        if (recentDays != null && recentDays < 1) {
            recentDays = null;
        }

        int safeLimit = safeRequest.getLimit() == null ? 10 : Math.min(Math.max(safeRequest.getLimit(), 1), 20);
        log.info(
                "agent_search_orders guestId={} reservationId={} city={} district={} hotelName={} roomTypeId={} roomTypeKeyword={} status={} limit={} sort={}",
                guestId,
                safeRequest.getReservationId(),
                normalizeCityKeyword(safeRequest.getCity()),
                normalizeDistrictKeyword(safeRequest.getDistrict()),
                normalizeHotelKeyword(safeRequest.getHotelName()),
                safeRequest.getRoomTypeId(),
                normalizeRoomTypeKeyword(safeRequest.getRoomTypeKeyword()),
                normalizeOrderStatus(safeRequest.getStatus()),
                safeLimit,
                normalizeSort(safeRequest.getSort())
        );
        List<AgentOrderSummaryVO> orders = agentToolMapper.searchOrders(
                guestId,
                safeRequest.getReservationId(),
                recentDays,
                normalizeProvinceKeyword(safeRequest.getProvince()),
                normalizeCityKeyword(safeRequest.getCity()),
                normalizeDistrictKeyword(safeRequest.getDistrict()),
                normalizeHotelKeyword(safeRequest.getHotelName()),
                safeRequest.getRoomTypeId(),
                normalizeRoomTypeKeyword(safeRequest.getRoomTypeKeyword()),
                normalizeOrderStatus(safeRequest.getStatus()),
                safeLimit,
                normalizeSort(safeRequest.getSort())
        );
        log.info("agent_search_orders_result hitCount={}", orders.size());
        return orders;
    }

    @Override
    public AgentOrderDetailVO getOrderDetail(Long reservationId, Long guestId) {
        if (reservationId == null || reservationId <= 0) {
            throw new RuntimeException("reservationId cannot be empty");
        }
        AgentOrderDetailVO detail = agentToolMapper.selectOrderDetail(reservationId);
        if (detail == null) {
            throw new RuntimeException("Order not found");
        }
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentOrderDetailVO updateOrder(Long reservationId, AgentOrderUpdateRequest request) {
        if (request == null) {
            throw new RuntimeException("Request body cannot be empty");
        }

        Reservation reservation = validateOrderOwnership(reservationId, request.getGuestId());
        if (enforceOrderStatus && !isEditableStatus(reservation.getStatus())) {
            throw new RuntimeException("The current order status does not allow updates.");
        }

        LocalDate nextCheckInDate = request.getCheckInDate() != null ? request.getCheckInDate() : reservation.getCheckInDate();
        LocalDate nextCheckOutDate = request.getCheckOutDate() != null ? request.getCheckOutDate() : reservation.getCheckOutDate();
        Long nextRoomTypeId = request.getRoomTypeId() != null ? request.getRoomTypeId() : reservation.getTypeId();
        if (request.getRoomTypeId() == null && StringUtils.hasText(request.getRoomTypeKeyword())) {
            nextRoomTypeId = resolveRoomTypeId(reservation.getHotelId(), request.getRoomTypeKeyword());
        }

        log.info(
                "agent_update_order reservationId={} guestId={} hotelId={} checkInDate={} checkOutDate={} requestedRoomTypeId={} requestedRoomTypeKeyword={} resolvedRoomTypeId={}",
                reservationId,
                request.getGuestId(),
                reservation.getHotelId(),
                nextCheckInDate,
                nextCheckOutDate,
                request.getRoomTypeId(),
                request.getRoomTypeKeyword(),
                nextRoomTypeId
        );

        if (nextCheckInDate == null || nextCheckOutDate == null || !nextCheckOutDate.isAfter(nextCheckInDate)) {
            throw new RuntimeException("Check-out date must be after check-in date.");
        }

        RoomType roomType = roomTypeService.getById(nextRoomTypeId);
        if (roomType == null) {
            throw new RuntimeException("Target room type does not exist.");
        }

        long stayDays = ChronoUnit.DAYS.between(nextCheckInDate, nextCheckOutDate);
        BigDecimal roomPrice = roomType.getPrice() == null ? BigDecimal.ZERO : roomType.getPrice();
        BigDecimal totalPrice = roomPrice
                .multiply(BigDecimal.valueOf(Math.max(stayDays, 1)))
                .multiply(BigDecimal.valueOf(reservation.getRoomCount()))
                .setScale(2, RoundingMode.HALF_UP);

        reservation.setCheckInDate(nextCheckInDate);
        reservation.setCheckOutDate(nextCheckOutDate);
        reservation.setTypeId(nextRoomTypeId);
        reservation.setTotalPrice(totalPrice);
        reservation.setOccupiedRooms(reservation.getOccupiedRooms() == null ? 0 : reservation.getOccupiedRooms());

        boolean updated = reservationMapper.updateById(reservation);
        if (!updated) {
            throw new RuntimeException("Failed to update the order.");
        }

        return getOrderDetail(reservationId, request.getGuestId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentOrderDetailVO cancelOrder(Long reservationId, AgentOrderCancelRequest request) {
        Reservation reservation = validateOrderOwnership(reservationId, request == null ? null : request.getGuestId());

        reservation.setStatus(ReservationEnum.CANCELED.getCode());
        reservation.setOccupiedRooms(reservation.getOccupiedRooms() == null ? 0 : reservation.getOccupiedRooms());
        boolean updated = reservationMapper.updateById(reservation);
        if (!updated) {
            throw new RuntimeException("Failed to cancel the order.");
        }

        return getOrderDetail(reservationId, request == null ? null : request.getGuestId());
    }

    private void enrichMatchedRoomTypes(AgentHotelSearchVO hotel) {
        if (hotel == null) {
            return;
        }
        String raw = hotel.getMatchedRoomTypesRaw();
        if (!StringUtils.hasText(raw)) {
            hotel.setMatchedRoomTypes(Collections.emptyList());
            return;
        }

        String[] parts = raw.split(",");
        List<String> matchedRoomTypes = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                matchedRoomTypes.add(part.trim());
            }
        }
        hotel.setMatchedRoomTypes(matchedRoomTypes);
    }

    private List<String> normalizeFacilities(List<String> required) {
        if (required == null || required.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String item : required) {
            String next = normalizeFacilityName(item);
            if (next != null) {
                normalized.add(next);
            }
        }
        return new ArrayList<>(normalized);
    }

    private String normalizeFacilityName(String raw) {
        String value = normalizeText(raw);
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");
        if (normalized.contains("wifi") || normalized.contains("无线网") || normalized.contains("无线网络")) {
            return "wifi";
        }
        if (normalized.contains("早餐")
                || normalized.contains("早饭")
                || normalized.contains("含早")
                || normalized.contains("带早餐")
                || normalized.contains("提供早餐")
                || normalized.contains("breakfast")) {
            return "breakfast";
        }
        if (normalized.contains("停车场")
                || normalized.contains("可停车")
                || normalized.contains("停车位")
                || normalized.contains("有停车场")
                || normalized.contains("parking")) {
            return "parking";
        }
        return null;
    }

    private String normalizeOrderStatus(String rawStatus) {
        String status = normalizeText(rawStatus);
        if (status == null) {
            return null;
        }

        String normalized = status.toLowerCase(Locale.ROOT);
        if ("booked".equals(normalized)
                || "created".equals(normalized)
                || "confirmed".equals(normalized)
                || normalized.contains("预订")
                || normalized.contains("待入住")
                || normalized.contains("已创建")
                || normalized.contains("已确认")) {
            return "booked";
        }
        if ("cancelled".equals(normalized)
                || "canceled".equals(normalized)
                || normalized.contains("取消")) {
            return "cancelled";
        }
        if ("completed".equals(normalized) || normalized.contains("完成")) {
            return "completed";
        }
        if ("checked_in".equals(normalized)
                || "checkedin".equals(normalized)
                || normalized.contains("入住")) {
            return "checked_in";
        }
        return null;
    }

    private String normalizeSort(String rawSort) {
        String sort = normalizeText(rawSort);
        if (sort == null) {
            return "createdAt_desc";
        }
        return sort;
    }

    private Long resolveRoomTypeId(Long hotelId, String roomTypeKeyword) {
        String keyword = normalizeRoomTypeKeyword(roomTypeKeyword);
        if (hotelId == null || !StringUtils.hasText(keyword)) {
            return null;
        }

        Long matchedRoomTypeId = agentToolMapper.selectRoomTypeIdByKeyword(hotelId, keyword);
        if (matchedRoomTypeId == null) {
            throw new RuntimeException("Target room type does not exist.");
        }
        return matchedRoomTypeId;
    }

    private String normalizeFacilityMatchMode(String rawMode, int facilitySize) {
        if (facilitySize <= 0) {
            return null;
        }
        String mode = normalizeText(rawMode);
        if (mode == null) {
            return "all";
        }
        String normalized = mode.toLowerCase(Locale.ROOT);
        if ("any".equals(normalized) || "at_least".equals(normalized) || "all".equals(normalized)) {
            return normalized;
        }
        return "all";
    }

    private Integer normalizeFacilityMinMatchCount(String matchMode, Integer rawCount, int facilitySize) {
        if (facilitySize <= 0 || matchMode == null) {
            return null;
        }
        if ("any".equals(matchMode)) {
            return 1;
        }
        if ("all".equals(matchMode)) {
            return facilitySize;
        }
        int count = rawCount == null ? 1 : rawCount;
        return Math.max(1, Math.min(count, facilitySize));
    }

    private Reservation validateOrderOwnership(Long reservationId, Long guestId) {
        if (reservationId == null || reservationId <= 0) {
            throw new RuntimeException("reservationId cannot be empty");
        }

        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new RuntimeException("Order not found");
        }
        if (enforceOrderOwnership && guestId != null && !guestId.equals(reservation.getGuestId())) {
            throw new RuntimeException("Order not found or not accessible");
        }
        return reservation;
    }

    private boolean isEditableStatus(Integer status) {
        return ReservationEnum.CREATED.getCode().equals(status) || ReservationEnum.CONFIRMED.getCode().equals(status);
    }

    private boolean hasAnyHotelSearchCondition(
            String province,
            String city,
            String district,
            String districtKeyword,
            String street,
            String addressKeyword,
            String hotelName,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal rating,
            Long roomTypeId,
            String roomTypeKeyword,
            Integer facilityCount
    ) {
        return StringUtils.hasText(province)
                || StringUtils.hasText(city)
                || StringUtils.hasText(district)
                || StringUtils.hasText(districtKeyword)
                || StringUtils.hasText(street)
                || StringUtils.hasText(addressKeyword)
                || StringUtils.hasText(hotelName)
                || minPrice != null
                || maxPrice != null
                || rating != null
                || roomTypeId != null
                || StringUtils.hasText(roomTypeKeyword)
                || facilityCount != null;
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private String normalizeProvinceKeyword(String text) {
        return stripCommonSuffix(normalizeText(text), "省", "市", "自治区", "特别行政区");
    }

    private String normalizeCityKeyword(String text) {
        return stripCommonSuffix(normalizeText(text), "市", "地区", "自治州", "盟");
    }

    private String normalizeDistrictKeyword(String text) {
        return stripCommonSuffix(normalizeText(text), "区", "县", "旗", "新区", "开发区");
    }

    private String normalizeHotelKeyword(String text) {
        return stripCommonSuffix(normalizeText(text), "酒店", "宾馆", "旅馆", "民宿", "客栈", "公寓", "度假村", "饭店", "门店", "店");
    }

    private String normalizeRoomTypeKeyword(String text) {
        String normalized = normalizeText(text);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalized
                .replace("房间类型", "")
                .replace("房型", "")
                .replace("类型", "")
                .trim();
    }

    private String stripCommonSuffix(String text, String... suffixes) {
        String normalized = normalizeText(text);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String suffix : suffixes) {
                if (StringUtils.hasText(suffix) && normalized.endsWith(suffix) && normalized.length() > suffix.length()) {
                    normalized = normalized.substring(0, normalized.length() - suffix.length()).trim();
                    changed = true;
                    break;
                }
            }
        }
        return StringUtils.hasText(normalized) ? normalized : normalizeText(text);
    }
}

