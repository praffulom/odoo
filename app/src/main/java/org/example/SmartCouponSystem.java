import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Smart Coupon Management System for e-commerce/retail platforms
 * 
 * <p>This system provides a comprehensive solution for managing coupon campaigns,
 * validating and redeeming coupons, detecting fraud attempts, generating personalized
 * coupons, and generating admin reports.</p>
 * 
 * <p>Key features:
 * <ul>
 *   <li>Secure coupon code generation with prefix patterns</li>
 *   <li>User segmentation (new/returning/all users)</li>
 *   <li>Product category filtering</li>
 *   <li>Minimum cart value requirements</li>
 *   <li>Fraud detection with IP-based lockout</li>
 *   <li>Personalized coupon generation based on user history</li>
 *   <li>Admin reporting capabilities</li>
 * </ul>
 * </p>
 */
public class SmartCouponSystem {
    // Main storage components
    private final Map<String, Coupon> couponStorage = new ConcurrentHashMap<>();
    private final Map<String, CouponCampaign> campaignStorage = new ConcurrentHashMap<>();
    private final Map<String, User> userStorage = new ConcurrentHashMap<>();
    private final List<FraudAttempt> fraudLog = new CopyOnWriteArrayList<>();
    private final Map<String, Cart> abandonedCarts = new ConcurrentHashMap<>();

    /**
     * Represents a coupon with validation rules
     */
    public static class Coupon {
        private final String code;
        private final String campaignId;
        private final String userId;
        private final LocalDateTime expiration;
        private final AtomicInteger useCount = new AtomicInteger(0);
        private final int maxUses;
        private volatile boolean valid = true;

        /**
         * Creates a new coupon
         * 
         * @param code Unique coupon code
         * @param campaignId ID of the campaign this coupon belongs to
         * @param userId ID of the user this coupon is assigned to
         * @param expiration Expiration date/time of the coupon
         * @param maxUses Maximum number of times this coupon can be used
         * 
         * @throws IllegalArgumentException if any parameter is invalid
         */
        public Coupon(String code, String campaignId, String userId, 
                      LocalDateTime expiration, int maxUses) {
            if (code == null || code.isBlank()) {
                throw new IllegalArgumentException("Coupon code cannot be null or blank");
            }
            if (campaignId == null || campaignId.isBlank()) {
                throw new IllegalArgumentException("Campaign ID cannot be null or blank");
            }
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("User ID cannot be null or blank");
            }
            if (expiration == null) {
                throw new IllegalArgumentException("Expiration cannot be null");
            }
            if (maxUses <= 0) {
                throw new IllegalArgumentException("Max uses must be positive");
            }
            
            this.code = code;
            this.campaignId = campaignId;
            this.userId = userId;
            this.expiration = expiration;
            this.maxUses = maxUses;
        }

        // Getters
        public String getCode() { return code; }
        public String getCampaignId() { return campaignId; }
        public String getUserId() { return userId; }
        public LocalDateTime getExpiration() { return expiration; }
        public int getUseCount() { return useCount.get(); }
        public int getMaxUses() { return maxUses; }
        public boolean isValid() { return valid; }
        
        /** Invalidates the coupon */
        public void invalidate() { valid = false; }
        
        /**
         * Increments the use count of the coupon
         * 
         * @return true if the coupon can still be used, false if max uses reached
         */
        public boolean incrementUse() { 
            return useCount.incrementAndGet() <= maxUses; 
        }
    }

    /**
     * Coupon campaign definition
     */
    public static class CouponCampaign {
        private final String id;
        private final UserSegment userSegment;
        private final String categoryFilter;
        private final double minCartValue;
        private final LocalDateTime expiration;
        private final int maxUsesPerUser;
        private final String prefixPattern;

        /**
         * Creates a new coupon campaign
         * 
         * @param id Unique campaign ID
         * @param userSegment Target user segment
         * @param categoryFilter Product category filter
         * @param minCartValue Minimum cart value required for redemption
         * @param expiration Campaign expiration date/time
         * @param maxUsesPerUser Maximum uses per user
         * @param prefixPattern Prefix pattern for coupon codes
         * 
         * @throws IllegalArgumentException if any parameter is invalid
         */
        public CouponCampaign(String id, UserSegment userSegment, String categoryFilter, 
                              double minCartValue, LocalDateTime expiration, 
                              int maxUsesPerUser, String prefixPattern) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Campaign ID cannot be null or blank");
            }
            if (userSegment == null) {
                throw new IllegalArgumentException("User segment cannot be null");
            }
            if (minCartValue < 0) {
                throw new IllegalArgumentException("Min cart value cannot be negative");
            }
            if (expiration == null) {
                throw new IllegalArgumentException("Expiration cannot be null");
            }
            if (maxUsesPerUser <= 0) {
                throw new IllegalArgumentException("Max uses per user must be positive");
            }
            
            this.id = id;
            this.userSegment = userSegment;
            this.categoryFilter = categoryFilter;
            this.minCartValue = minCartValue;
            this.expiration = expiration;
            this.maxUsesPerUser = maxUsesPerUser;
            this.prefixPattern = prefixPattern;
        }

        // Getters
        public String getId() { return id; }
        public UserSegment getUserSegment() { return userSegment; }
        public String getCategoryFilter() { return categoryFilter; }
        public double getMinCartValue() { return minCartValue; }
        public LocalDateTime getExpiration() { return expiration; }
        public int getMaxUsesPerUser() { return maxUsesPerUser; }
        public String getPrefixPattern() { return prefixPattern; }
    }

    /**
     * User segments
     */
    public enum UserSegment {
        NEW_USER, RETURNING_USER, ALL_USERS
    }

    /**
     * User representation
     */
    public static class User {
        private final String id;
        private final boolean isNew;
        private final int pastOrderCount;
        private final double totalSpent;
        private final Set<String> redeemedCoupons = ConcurrentHashMap.newKeySet();

        /**
         * Creates a new user
         * 
         * @param id Unique user ID
         * @param isNew Whether the user is new
         * @param pastOrderCount Number of past orders
         * @param totalSpent Total amount spent by user
         * 
         * @throws IllegalArgumentException if any parameter is invalid
         */
        public User(String id, boolean isNew, int pastOrderCount, double totalSpent) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("User ID cannot be null or blank");
            }
            if (pastOrderCount < 0) {
                throw new IllegalArgumentException("Past order count cannot be negative");
            }
            if (totalSpent < 0) {
                throw new IllegalArgumentException("Total spent cannot be negative");
            }
            
            this.id = id;
            this.isNew = isNew;
            this.pastOrderCount = pastOrderCount;
            this.totalSpent = totalSpent;
        }

        // Getters
        public String getId() { return id; }
        public boolean isNew() { return isNew; }
        public int getPastOrderCount() { return pastOrderCount; }
        public double getTotalSpent() { return totalSpent; }
        public Set<String> getRedeemedCoupons() { return redeemedCoupons; }
        
        /**
         * Adds a redeemed coupon to the user's history
         * 
         * @param couponCode Code of the redeemed coupon
         * 
         * @throws IllegalArgumentException if couponCode is null or blank
         */
        public void addRedeemedCoupon(String couponCode) {
            if (couponCode == null || couponCode.isBlank()) {
                throw new IllegalArgumentException("Coupon code cannot be null or blank");
            }
            redeemedCoupons.add(couponCode);
        }
    }

    /**
     * Shopping cart representation
     */
    public static class Cart {
        private final String userId;
        private final Map<String, Double> items; // itemId -> price
        private final double totalValue;
        private final LocalDateTime lastUpdated;

        /**
         * Creates a new shopping cart
         * 
         * @param userId ID of the user who owns the cart
         * @param items Map of item IDs to prices
         * 
         * @throws IllegalArgumentException if any parameter is invalid
         */
        public Cart(String userId, Map<String, Double> items) {
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("User ID cannot be null or blank");
            }
            if (items == null) {
                throw new IllegalArgumentException("Items cannot be null");
            }
            if (items.values().stream().anyMatch(price -> price < 0)) {
                throw new IllegalArgumentException("Item price cannot be negative");
            }
            
            this.userId = userId;
            this.items = new ConcurrentHashMap<>(items);
            this.totalValue = items.values().stream().mapToDouble(Double::doubleValue).sum();
            this.lastUpdated = LocalDateTime.now();
        }

        // Getters
        public String getUserId() { return userId; }
        public Map<String, Double> getItems() { return Collections.unmodifiableMap(items); }
        public double getTotalValue() { return totalValue; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    /**
     * Fraud attempt record
     */
    public static class FraudAttempt {
        private final String couponCode;
        private final String userId;
        private final String ipAddress;
        private final String deviceFingerprint;
        private final LocalDateTime timestamp;
        private final boolean successful;

        /**
         * Creates a new fraud attempt record
         * 
         * @param couponCode Coupon code used in the attempt
         * @param userId ID of the user making the attempt
         * @param ipAddress IP address of the requester
         * @param deviceFingerprint Device fingerprint of the requester
         * @param successful Whether the attempt was successful
         * 
         * @throws IllegalArgumentException if any parameter is invalid
         */
        public FraudAttempt(String couponCode, String userId, String ipAddress, 
                           String deviceFingerprint, boolean successful) {
            if (couponCode == null || couponCode.isBlank()) {
                throw new IllegalArgumentException("Coupon code cannot be null or blank");
            }
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("User ID cannot be null or blank");
            }
            if (ipAddress == null || ipAddress.isBlank()) {
                throw new IllegalArgumentException("IP address cannot be null or blank");
            }
            if (deviceFingerprint == null || deviceFingerprint.isBlank()) {
                throw new IllegalArgumentException("Device fingerprint cannot be null or blank");
            }
            
            this.couponCode = couponCode;
            this.userId = userId;
            this.ipAddress = ipAddress;
            this.deviceFingerprint = deviceFingerprint;
            this.timestamp = LocalDateTime.now();
            this.successful = successful;
        }

        // Getters
        public String getCouponCode() { return couponCode; }
        public String getUserId() { return userId; }
        public String getIpAddress() { return ipAddress; }
        public String getDeviceFingerprint() { return deviceFingerprint; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean isSuccessful() { return successful; }
    }

    /**
     * Generates secure coupon codes
     * 
     * @param prefix Optional prefix for the coupon code
     * @return Generated coupon code
     */
    private String generateCouponCode(String prefix) {
        try {
            String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
            return (prefix != null && !prefix.isEmpty()) ? 
                prefix + "-" + uuid.substring(0, 12) : 
                uuid.substring(0, 16);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate coupon code", e);
        }
    }

    /**
     * Creates a new coupon campaign
     * 
     * @param campaign Campaign to create
     * 
     * @throws IllegalArgumentException if campaign is null or ID already exists
     */
    public void createCampaign(CouponCampaign campaign) {
        if (campaign == null) {
            throw new IllegalArgumentException("Campaign cannot be null");
        }
        if (campaignStorage.containsKey(campaign.getId())) {
            throw new IllegalArgumentException("Campaign with ID " + campaign.getId() + " already exists");
        }
        campaignStorage.put(campaign.getId(), campaign);
    }

    /**
     * Generates coupons for a campaign
     * 
     * @param campaignId ID of the campaign
     * @param userId ID of the user to assign the coupon to
     * @return Generated coupon, or null if campaign doesn't exist
     * 
     * @throws IllegalArgumentException if campaignId or userId is invalid
     */
    public Coupon generateCoupon(String campaignId, String userId) {
        if (campaignId == null || campaignId.isBlank()) {
            throw new IllegalArgumentException("Campaign ID cannot be null or blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        
        CouponCampaign campaign = campaignStorage.get(campaignId);
        if (campaign == null) return null;
        
        String code = generateCouponCode(campaign.getPrefixPattern());
        Coupon coupon = new Coupon(
            code, campaignId, userId, 
            campaign.getExpiration(), campaign.getMaxUsesPerUser()
        );
        
        couponStorage.put(code, coupon);
        return coupon;
    }

    /**
     * Validates and redeems a coupon (with optimistic locking)
     * 
     * @param couponCode Coupon code to validate
     * @param cart Shopping cart to validate against
     * @param ipAddress IP address of the requester
     * @param deviceFingerprint Device fingerprint of the requester
     * @return true if redemption was successful, false otherwise
     * 
     * @throws IllegalArgumentException if any parameter is invalid
     * @throws IllegalStateException if system is in inconsistent state
     */
    public synchronized boolean validateAndRedeem(String couponCode, Cart cart, 
                                                 String ipAddress, String deviceFingerprint) {
        // Input validation
        if (couponCode == null || couponCode.isBlank()) {
            throw new IllegalArgumentException("Coupon code cannot be null or blank");
        }
        if (cart == null) {
            throw new IllegalArgumentException("Cart cannot be null");
        }
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException("IP address cannot be null or blank");
        }
        if (deviceFingerprint == null || deviceFingerprint.isBlank()) {
            throw new IllegalArgumentException("Device fingerprint cannot be null or blank");
        }
        
        try {
            Coupon coupon = couponStorage.get(couponCode);
            if (coupon == null) {
                logFraudAttempt(couponCode, cart.getUserId(), ipAddress, deviceFingerprint, false);
                return false;
            }
            
            // 1. Check expiration
            if (coupon.getExpiration().isBefore(LocalDateTime.now())) {
                logFraudAttempt(couponCode, cart.getUserId(), ipAddress, deviceFingerprint, false);
                return false;
            }
            
            // 2. Check usage limits
            if (coupon.getUseCount() >= coupon.getMaxUses()) {
                logFraudAttempt(couponCode, cart.getUserId(), ipAddress, deviceFingerprint, false);
                return false;
            }
            
            // 3. Check cart value
            CouponCampaign campaign = campaignStorage.get(coupon.getCampaignId());
            if (campaign == null) {
                throw new IllegalStateException("Campaign not found for coupon: " + couponCode);
            }
            if (cart.getTotalValue() < campaign.getMinCartValue()) {
                logFraudAttempt(couponCode, cart.getUserId(), ipAddress, deviceFingerprint, false);
                return false;
            }
            
            // 4. Check user segment
            User user = userStorage.get(cart.getUserId());
            if (user == null) {
                throw new IllegalStateException("User not found: " + cart.getUserId());
            }
            if (!isUserInSegment(user, campaign.getUserSegment())) {
                logFraudAttempt(couponCode, cart.getUserId(), ipAddress, deviceFingerprint, false);
                return false;
            }
            
            // 5. Check fraud lockout
            if (isUserLockedOut(cart.getUserId(), ipAddress)) {
                logFraudAttempt(couponCode, cart.getUserId(), ipAddress, deviceFingerprint, false);
                return false;
            }
            
            // 6. Optimistic redemption attempt
            if (!coupon.incrementUse()) {
                logFraudAttempt(couponCode, cart.getUserId(), ipAddress, deviceFingerprint, false);
                return false;
            }
            
            // Successful redemption
            user.addRedeemedCoupon(couponCode);
            logFraudAttempt(couponCode, cart.getUserId(), ipAddress, deviceFingerprint, true);
            return true;
        } catch (Exception e) {
            // Log the exception and rethrow as runtime exception
            System.err.println("Error during coupon validation: " + e.getMessage());
            throw new RuntimeException("Coupon validation failed", e);
        }
    }

    private boolean isUserInSegment(User user, UserSegment segment) {
        return switch (segment) {
            case NEW_USER -> user.isNew();
            case RETURNING_USER -> !user.isNew();
            case ALL_USERS -> true;
        };
    }

    /**
     * Fraud detection and lockout mechanism
     */
    private void logFraudAttempt(String couponCode, String userId, String ipAddress, 
                                String deviceFingerprint, boolean successful) {
        try {
            fraudLog.add(new FraudAttempt(couponCode, userId, ipAddress, deviceFingerprint, successful));
        } catch (Exception e) {
            System.err.println("Failed to log fraud attempt: " + e.getMessage());
        }
    }

    /**
     * Checks if a user is locked out due to fraud attempts
     * 
     * @param userId User ID to check
     * @param ipAddress IP address to check
     * @return true if user is locked out, false otherwise
     */
    public boolean isUserLockedOut(String userId, String ipAddress) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or blank");
        }
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException("IP address cannot be null or blank");
        }
        
        try {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            
            long failedAttempts = fraudLog.stream()
                .filter(attempt -> !attempt.isSuccessful())
                .filter(attempt -> attempt.getTimestamp().isAfter(oneHourAgo))
                .filter(attempt -> 
                    attempt.getUserId().equals(userId) || 
                    attempt.getIpAddress().equals(ipAddress))
                .count();
                
            return failedAttempts >= 5;
        } catch (Exception e) {
            System.err.println("Error checking user lockout: " + e.getMessage());
            return false;
        }
    }

    /**
     * Personalization engine for abandoned carts
     * 
     * @param user User to generate coupons for
     * @return List of generated coupons (may be empty)
     * 
     * @throws IllegalArgumentException if user is null
     */
    public List<Coupon> generatePersonalizedCoupons(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        List<Coupon> coupons = new ArrayList<>();
        
        try {
            // Check if user qualifies for personalized coupons
            if (user.getPastOrderCount() >= 3 || user.getTotalSpent() >= 500) {
                // Find abandoned cart
                Cart cart = abandonedCarts.get(user.getId());
                if (cart != null) {
                    // Find campaigns matching cart categories
                    campaignStorage.values().stream()
                        .filter(campaign -> 
                            cart.getItems().keySet().stream()
                                .anyMatch(item -> item.startsWith(campaign.getCategoryFilter())))
                        .forEach(campaign -> {
                            Coupon coupon = generateCoupon(campaign.getId(), user.getId());
                            if (coupon != null) coupons.add(coupon);
                        });
                }
            }
        } catch (Exception e) {
            System.err.println("Error generating personalized coupons: " + e.getMessage());
        }
        
        return coupons;
    }

    // Admin reporting methods with improved documentation and error handling
    
    /**
     * Gets all active campaigns
     * 
     * @return List of active campaigns
     */
    public List<CouponCampaign> getActiveCampaigns() {
        try {
            LocalDateTime now = LocalDateTime.now();
            return campaignStorage.values().stream()
                .filter(campaign -> campaign.getExpiration().isAfter(now))
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting active campaigns: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Gets all expired campaigns
     * 
     * @return List of expired campaigns
     */
    public List<CouponCampaign> getExpiredCampaigns() {
        try {
            LocalDateTime now = LocalDateTime.now();
            return campaignStorage.values().stream()
                .filter(campaign -> campaign.getExpiration().isBefore(now))
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting expired campaigns: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Gets top redeemed coupons
     * 
     * @param limit Maximum number of coupons to return
     * @return List of top redeemed coupons
     * 
     * @throws IllegalArgumentException if limit is not positive
     */
    public List<Coupon> getTopRedeemedCoupons(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be positive");
        }
        
        try {
            return couponStorage.values().stream()
                .filter(Coupon::isValid)
                .sorted(Comparator.comparingInt(Coupon::getUseCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting top coupons: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Gets all flagged fraud events
     * 
     * @return List of fraud attempts that were not successful
     */
    public List<FraudAttempt> getFlaggedFraudEvents() {
        try {
            return fraudLog.stream()
                .filter(attempt -> !attempt.isSuccessful())
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting fraud events: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Adds a user to the system
     * 
     * @param user User to add
     * 
     * @throws IllegalArgumentException if user is null or ID already exists
     */
    public void addUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (userStorage.containsKey(user.getId())) {
            throw new IllegalArgumentException("User with ID " + user.getId() + " already exists");
        }
        userStorage.put(user.getId(), user);
    }

    /**
     * Adds an abandoned cart to the system
     * 
     * @param cart Cart to add
     * 
     * @throws IllegalArgumentException if cart is null or user already has an abandoned cart
     */
    public void addAbandonedCart(Cart cart) {
        if (cart == null) {
            throw new IllegalArgumentException("Cart cannot be null");
        }
        if (abandonedCarts.containsKey(cart.getUserId())) {
            throw new IllegalArgumentException("User already has an abandoned cart");
        }
        abandonedCarts.put(cart.getUserId(), cart);
    }

    /**
     * Test helper method to add a coupon directly to storage
     * 
     * @param coupon Coupon to add
     * 
     * @throws IllegalArgumentException if coupon is null or code already exists
     */
    public void addCouponForTesting(Coupon coupon) {
        if (coupon == null) {
            throw new IllegalArgumentException("Coupon cannot be null");
        }
        if (couponStorage.containsKey(coupon.getCode())) {
            throw new IllegalArgumentException("Coupon with code " + coupon.getCode() + " already exists");
        }
        couponStorage.put(coupon.getCode(), coupon);
    }

    // Enhanced test harness with error scenarios
    public static void main(String[] args) {
        SmartCouponSystem system = new SmartCouponSystem();
        
        try {
            // Create sample campaign
            CouponCampaign campaign = new CouponCampaign(
                "SUMMER2025", 
                UserSegment.RETURNING_USER,
                "ELECTRONICS",
                100.0,
                LocalDateTime.now().plusDays(30),
                3,
                "SUMMER"
            );
            system.createCampaign(campaign);
            
            // Create sample user
            User user = new User("user123", false, 5, 1200.0);
            system.addUser(user);
            
            // Generate coupon
            Coupon coupon = system.generateCoupon("SUMMER2025", "user123");
            System.out.println("Generated coupon: " + coupon.getCode());
            
            // Create cart
            Map<String, Double> cartItems = new HashMap<>();
            cartItems.put("ELECTRONICS-LAPTOP", 1200.0);
            Cart cart = new Cart("user123", cartItems);
            
            // Validate and redeem
            boolean success = system.validateAndRedeem(
                coupon.getCode(), cart, "192.168.1.1", "device-fingerprint-123"
            );
            System.out.println("Redemption " + (success ? "successful" : "failed"));
            
            // Generate personalized coupons
            system.addAbandonedCart(cart);
            List<Coupon> personalized = system.generatePersonalizedCoupons(user);
            System.out.println("Personalized coupons generated: " + personalized.size());
            
            // Admin reports
            System.out.println("Active campaigns: " + system.getActiveCampaigns().size());
            System.out.println("Top coupons: " + system.getTopRedeemedCoupons(5).size());
            
            // Test error scenarios
            System.out.println("\nTesting error scenarios:");
            
            // Invalid coupon redemption
            try {
                system.validateAndRedeem("INVALID_CODE", cart, "192.168.1.1", "device-123");
            } catch (Exception e) {
                System.out.println("Handled invalid coupon: " + e.getMessage());
            }
            
            // Invalid user
            try {
                system.addUser(new User("", false, 5, 100.0));
            } catch (IllegalArgumentException e) {
                System.out.println("Handled invalid user: " + e.getMessage());
            }
            
            // Expired coupon
            Coupon expiredCoupon = new Coupon(
                "EXPIRED", "SUMMER2025", "user123", 
                LocalDateTime.now().minusDays(1), 1
            );
            system.addCouponForTesting(expiredCoupon);
            success = system.validateAndRedeem(
                "EXPIRED", cart, "192.168.1.1", "device-fingerprint-123"
            );
            System.out.println("Expired coupon redemption: " + (success ? "successful" : "failed"));
            
        } catch (Exception e) {
            System.err.println("System test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 