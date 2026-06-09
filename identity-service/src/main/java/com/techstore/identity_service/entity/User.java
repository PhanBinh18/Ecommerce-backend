package com.techstore.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * users table
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"roles", "addresses"})
@ToString(exclude = {"roles", "addresses"})
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fullName;

    private String phone;

    /**
     * Trạng thái active của account
     */
    private boolean isActive = true;

    /**
     * Thời điểm tạo user, dùng @CreationTimestamp để Hibernate set tự động
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Quan hệ users - roles (n-n)
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Quan hệ users - addresses (1-n)
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Address> addresses = new HashSet<>();

    // ===========================
    // UserDetails implementation
    // ===========================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.isEmpty()) return Collections.emptyList();
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsername() {
        return email;
    }

    // password getter already provided by Lombok

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Keep existing behavior: use isActive to indicate non-locked/enabled.
     * You may separate isLocked and isActive if you want different semantics.
     */
    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    // Convenience helpers to manage bi-directional relation with Address
    public void addAddress(Address address) {
        address.setUser(this);
        this.addresses.add(address);
    }

    public void removeAddress(Address address) {
        address.setUser(null);
        this.addresses.remove(address);
    }
}