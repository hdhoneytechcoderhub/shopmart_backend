package com.shopmart.module.settings.entity;

import com.shopmart.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "settings")
public class Setting extends BaseEntity {
    @Id
    @Column(name = "setting_key", length = 120)
    private String key;

    @Column(name = "setting_value", length = 4000)
    private String value;

    /** Public settings are exposed to the storefront via /settings/public. */
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;
}
