package com.shopmart.module.home.controller;

import com.shopmart.common.dto.ApiResponse;
import com.shopmart.module.banner.service.BannerService;
import com.shopmart.module.category.service.CategoryService;
import com.shopmart.module.home.dto.HomeResponse;
import com.shopmart.module.product.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public landing-page aggregation: banners + categories + featured + latest in one response. */
@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
@Tag(name = "Home")
public class HomeController {

    private final BannerService bannerService;
    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping
    public ApiResponse<HomeResponse> home(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.ok(new HomeResponse(
                bannerService.listActive(),
                categoryService.getAll(),
                productService.featured(limit),
                productService.latest(limit)
        ));
    }
}
