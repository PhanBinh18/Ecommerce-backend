package com.techstore.identity_service.service;

import com.techstore.identity_service.dto.AddressRequest;
import com.techstore.identity_service.dto.AddressResponse;
import com.techstore.identity_service.dto.UserProfileResponse;
import com.techstore.identity_service.entity.Address;
import com.techstore.identity_service.entity.Role;
import com.techstore.identity_service.entity.User;
import com.techstore.identity_service.repository.AddressRepository;
import com.techstore.identity_service.repository.UserRepository;
import com.techstore.identity_service.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.techstore.identity_service.repository.RoleRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UserService: contains user CRUD + profile + address operations.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository,
                       AddressRepository addressRepository,
                       RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
        this.roleRepository = roleRepository;
    }

    // ---------- existing methods (kept) ----------

    // Đăng ký User mới (original createUser) - note: prefer AuthService for register flows
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }
        return userRepository.save(user);
    }

    // Lấy thông tin User theo ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + id));
    }

    // Lấy danh sách toàn bộ User (Cho Admin)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ---------- profile & address related methods ----------

    /**
     * Get profile of currently authenticated user.
     */
    public UserProfileResponse getCurrentUserProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = getUserById(userId);
        return mapToUserProfileResponse(user);
    }

    /**
     * Update profile (fullName, phone) of currently authenticated user.
     */
    @Transactional
    public UserProfileResponse updateCurrentUserProfile(String fullName, String phone) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = getUserById(userId);

        boolean modified = false;
        if (fullName != null) {
            user.setFullName(fullName);
            modified = true;
        }
        if (phone != null) {
            user.setPhone(phone);
            modified = true;
        }

        if (modified) {
            userRepository.save(user);
        }

        return mapToUserProfileResponse(user);
    }

    /**
     * List addresses for current user.
     */
    public List<AddressResponse> getAddressesForCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Address> addresses = addressRepository.findByUserId(userId);
        return addresses.stream()
                .sorted(Comparator.comparing(Address::isDefault).reversed()) // default first
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new address for current user.
     * If addressRequest.isDefault == true, unset other defaults.
     */
    @Transactional
    public AddressResponse createAddress(AddressRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = getUserById(userId);

        Address addr = new Address();
        addr.setStreet(request.getStreet());
        addr.setCity(request.getCity());
        addr.setDefault(Boolean.TRUE.equals(request.getIsDefault()));
        addr.setUser(user);

        Address saved = addressRepository.save(addr);

        // If this address is default, unset other defaults
        if (saved.isDefault()) {
            unsetOtherDefaults(userId, saved.getId());
        }

        return mapToAddressResponse(saved);
    }

    /**
     * Update an existing address belonging to current user.
     */
    @Transactional
    public AddressResponse updateAddress(Long addressId, AddressRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Address addr = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy address với id: " + addressId));

        if (!addr.getUser().getId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa địa chỉ này");
        }

        if (request.getStreet() != null) addr.setStreet(request.getStreet());
        if (request.getCity() != null) addr.setCity(request.getCity());

        Boolean reqDefault = request.getIsDefault();
        if (reqDefault != null) {
            addr.setDefault(reqDefault);
        }

        Address saved = addressRepository.save(addr);

        if (saved.isDefault()) {
            unsetOtherDefaults(userId, saved.getId());
        }

        return mapToAddressResponse(saved);
    }

    // ---------- helpers ----------

    /**
     * Unset isDefault for other addresses of the user (exclude idToKeep).
     */
    private void unsetOtherDefaults(Long userId, Long idToKeep) {
        List<Address> others = addressRepository.findByUserId(userId)
                .stream()
                .filter(a -> !a.getId().equals(idToKeep) && a.isDefault())
                .collect(Collectors.toList());

        if (!others.isEmpty()) {
            others.forEach(a -> a.setDefault(false));
            addressRepository.saveAll(others);
        }
    }

    private AddressResponse mapToAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .city(address.getCity())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .roles(user.getRoles() == null ? null :
                        user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()));

        // map addresses
        List<AddressResponse> addressResponses = user.getAddresses() == null ? List.of()
                : user.getAddresses().stream()
                .sorted(Comparator.comparing(Address::isDefault).reversed())
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());

        builder.addresses(addressResponses);
        return builder.build();
    }

    // ---------- admin methods ----------

    /**
     * Search users by email or fullName (admin only).
     * If keyword is null/empty, return all users.
     */
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllUsers();
        }

        String lowerKeyword = keyword.toLowerCase();
        return getAllUsers().stream()
                .filter(u -> u.getEmail().toLowerCase().contains(lowerKeyword)
                        || (u.getFullName() != null && u.getFullName().toLowerCase().contains(lowerKeyword)))
                .collect(Collectors.toList());
    }

    /**
     * Toggle isActive status of a user (admin only).
     */
    @Transactional
    public User toggleUserStatus(Long userId) {
        User user = getUserById(userId);
        user.setActive(!user.isActive());
        return userRepository.save(user);
    }

    /**
     * Update role of a user (admin only).
     * roleName: "ROLE_ADMIN" or "ROLE_CUSTOMER"
     */
    @Transactional
    public User updateUserRole(Long userId, String roleName) {
        User user = getUserById(userId);

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role không tồn tại: " + roleName));

        // Clear existing roles and set new one (if you want single role per user)
        // Or add to existing roles if you want multiple
        // Here: we replace roles with the new one
        user.getRoles().clear();
        user.getRoles().add(role);

        return userRepository.save(user);
    }
}