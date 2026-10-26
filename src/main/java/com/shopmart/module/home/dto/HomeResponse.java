package com.shopmart.module.home.dto;

import com.shopmart.module.banner.dto.BannerResponse;
import com.shopmart.module.category.dto.CategoryResponse;
import com.shopmart.module.product.dto.ProductSummary;

import java.util.List;

/** Aggregated payload for the storefront landing page (one call). */
public record HomeResponse(
        List<BannerResponse> banners,
        List<CategoryResponse> categories,
        List<ProductSummary> featured,
        List<ProductSummary> latest
) {}
