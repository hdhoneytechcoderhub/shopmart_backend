package com.shopmart.module.category.service.impl;

import com.shopmart.common.exception.BadRequestException;
import com.shopmart.common.exception.ConflictException;
import com.shopmart.common.exception.ResourceNotFoundException;
import com.shopmart.module.category.dto.CategoryRequest;
import com.shopmart.module.category.dto.CategoryResponse;
import com.shopmart.module.category.entity.Category;
import com.shopmart.module.category.mapper.CategoryMapper;
import com.shopmart.module.category.repository.CategoryRepository;
import com.shopmart.module.category.service.CategoryService;
import com.shopmart.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final int MAX_HIERARCHY_DEPTH = 10;

    private final CategoryRepository repository;

    @Override
    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String slug = SlugUtils.slugify(request.name());
        if (repository.existsBySlug(slug)) {
            throw new ConflictException("A category with a similar name already exists");
        }
        Category c = new Category();
        apply(c, request, slug, null);
        return CategoryMapper.toResponse(repository.save(c));
    }

    @Override
    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category c = find(id);
        apply(c, request, c.getSlug(), id);
        return CategoryMapper.toResponse(repository.save(c));
    }

    @Override
    @CacheEvict(cacheNames = "categories", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Category c = find(id);
        if (repository.countByParentId(id) > 0) {
            throw new BadRequestException(
                    "Cannot delete a category that has subcategories — reassign or delete them first");
        }
        repository.delete(c);
    }

    @Override
    @Cacheable(cacheNames = "categories", key = "'id:' + #id")
    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return CategoryMapper.toResponse(find(id));
    }

    @Override
    @Cacheable(cacheNames = "categories", key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return repository.findAll().stream().map(CategoryMapper::toResponse).toList();
    }

    @Override
    @Cacheable(cacheNames = "categories", key = "'children:' + (#parentId != null ? #parentId : 'root')")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildren(Long parentId) {
        List<Category> children = parentId == null
                ? repository.findByParentIdIsNullOrderBySortOrderAsc()
                : repository.findByParentIdOrderBySortOrderAsc(parentId);
        return children.stream().map(CategoryMapper::toResponse).toList();
    }

    private Category find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    private void apply(Category c, CategoryRequest r, String slug, Long selfId) {
        c.setName(r.name());
        c.setSlug(slug);
        c.setDescription(r.description());
        c.setBannerUrl(r.bannerUrl());
        c.setMetaTitle(r.metaTitle());
        c.setMetaDescription(r.metaDescription());
        c.setSortOrder(r.sortOrder() != null ? r.sortOrder() : 0);
        c.setActive(r.active() == null || r.active());
        c.setParentId(validateParent(r.parentId(), selfId));
    }

    /** Rejects a missing parent, direct self-parenting, and cycles up to MAX_HIERARCHY_DEPTH —
     *  deep enough for any real category tree; a longer chain is almost certainly a data error. */
    private Long validateParent(Long parentId, Long selfId) {
        if (parentId == null) return null;
        if (parentId.equals(selfId)) {
            throw new BadRequestException("A category cannot be its own parent");
        }
        Category parent = repository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", parentId));

        if (selfId != null) {
            Long cursor = parent.getParentId();
            for (int depth = 0; cursor != null && depth < MAX_HIERARCHY_DEPTH; depth++) {
                if (cursor.equals(selfId)) {
                    throw new BadRequestException("This would create a circular category hierarchy");
                }
                cursor = repository.findById(cursor).map(Category::getParentId).orElse(null);
            }
        }
        return parentId;
    }
}
