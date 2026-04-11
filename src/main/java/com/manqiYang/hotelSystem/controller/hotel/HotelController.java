package com.manqiYang.hotelSystem.controller.hotel;

import com.manqiYang.hotelSystem.common.Result;
import com.manqiYang.hotelSystem.entity.hotel.Hotel;
import com.manqiYang.hotelSystem.entity.hotel.HotelTag;
import com.manqiYang.hotelSystem.service.hotel.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/hotel")
@CrossOrigin(origins = "*")  // 允许所有前端访问
public class HotelController {

    @Autowired
    private HotelService hotelService;

    @GetMapping("/{id}")
    public Result get(@PathVariable Long id){
        return Result.success(hotelService.getById(id));
    }

    @GetMapping("/search")
    public Result getByName(@RequestParam String name){
        return Result.success(hotelService.getByName(name));
    }

    @GetMapping("/city/{city}")
    public Result getByCity(@PathVariable String city){
        return Result.success(hotelService.getByCity(city));
    }

    @GetMapping("/price")
    public Result getByPriceRange(@RequestParam BigDecimal minPrice, @RequestParam BigDecimal maxPrice){
        return Result.success(hotelService.getByPriceRange(minPrice, maxPrice));
    }

    @GetMapping("/all")
    public Result getAll(){
        return Result.success(hotelService.getAll());
    }

    @PostMapping("/register")
    public Result register(@RequestBody Hotel hotel){
        return Result.success(hotelService.createHotel(hotel));
    }

    @PutMapping("/update")
    public Result update(@RequestBody Hotel hotel){
        return Result.success(hotelService.updateHotel(hotel));
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id){
        return Result.success(hotelService.deleteHotel(id));
    }

    @DeleteMapping("/tagDel")
    public Result deleteTag(@RequestParam Long tagId){
        return Result.success(hotelService.deleteTag(tagId));
    }

    @PostMapping("/tag/add")
    public Result<HotelTag> addTag(@RequestBody HotelTag tag){
        return Result.success(hotelService.addHotelTag(tag));
    }

    @PutMapping("/tag/update")
    public Result updateTag(@RequestBody HotelTag tag){
        return Result.success(hotelService.updateHotelTag(tag));
    }

    @GetMapping("/{hotelId}/tags")
    public Result getTags(@PathVariable Long hotelId){
        return Result.success(hotelService.getTagsByHotelId(hotelId));
    }
}