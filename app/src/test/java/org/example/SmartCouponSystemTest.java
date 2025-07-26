package com.example;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Comprehensive test suite for SmartCouponSystem
 * Tests all classes, methods, and edge cases
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SmartCouponSystemTest {

    private SmartCouponSystem system;
    private SmartCouponSystem.User testUser;
    private SmartCouponSystem.User newUser;
    private SmartCouponSystem.CouponCampaign testCampaign;
    private SmartCouponSystem.Cart testCart;

    @BeforeEach
    void setUp() {
        system = new SmartCouponSystem();
        
        // Create test user
        testUser = new SmartCouponSystem.User("user123", false, 5, 1200.0);
        newUser = new SmartCouponSystem.User("newUser456", true, 0, 0.0);
        
        // Create test campaign
        testCampaign = new SmartCouponSystem.CouponCampaign(
            "TEST_CAMPAIGN",
            SmartCouponSystem.UserSegment.ALL_USERS,
            "ELECTRONICS",
            100.0,
            LocalDateTime.now().plusDays(30),
            3,
            "TEST"
        );
        
        // Create test cart
        Map<String, Double> cartItems = new HashMap<>();
        cartItems.put("ELECTRONICS-LAPTOP", 1200.0);
        cartItems.put("ELECTRONICS-MOUSE", 50.0);
        testCart = new SmartCouponSystem.Cart("user123", cartItems);
        
        // Add to system
        system.addUser(testUser);
        system.addUser(newUser);
        system.createCampaign(testCampaign);
    }

    // ===== COUPON CLASS TESTS =====
    
    @Test
    @Order(1)
    @DisplayName("Test Coupon creation with valid parameters")
    void testCouponCreation() {
        SmartCouponSystem.Coupon coupon = new SmartCouponSystem.Coupon(
            "TESTCODE123", "campaign1", "user1", 
            LocalDateTime.now().plusDays(30), 5
        );
        
        assertEquals("TESTCODE123", coupon.getCode());
        assertEquals("campaign1", coupon.getCampaignId());
        assertEquals("user1", coupon.getUserId());
        assertEquals(5, coupon.getMaxUses());
        assertEquals(0, coupon.getUseCount());
        assertTrue(coupon.isValid());
    }

    @Test
    @Order(2)
    @DisplayName("Test Coupon creation with invalid parameters")
    void testCouponCreationInvalidParameters() {
        // Null code
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.Coupon(null, "campaign1", "user1", LocalDateTime.now().plusDays(1), 1));
        
        // Blank code
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.Coupon("", "campaign1", "user1", LocalDateTime.now().plusDays(1), 1));
        
        // Null campaign ID
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.Coupon("CODE123", null, "user1", LocalDateTime.now().plusDays(1), 1));
        
        // Null user ID
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.Coupon("CODE123", "campaign1", null, LocalDateTime.now().plusDays(1), 1));
        
        // Null expiration
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.Coupon("CODE123", "campaign1", "user1", null, 1));
        
        // Zero max uses
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.Coupon("CODE123", "campaign1", "user1", LocalDateTime.now().plusDays(1), 0));
        
        // Negative max uses
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.Coupon("CODE123", "campaign1", "user1", LocalDateTime.now().plusDays(1), -1));
    }

    @Test
    @Order(3)
    @DisplayName("Test Coupon usage increment")
    void testCouponUsageIncrement() {
        SmartCouponSystem.Coupon coupon = new SmartCouponSystem.Coupon(
            "TESTCODE123", "campaign1", "user1", 
            LocalDateTime.now().plusDays(30), 2
        );
        
        assertTrue(coupon.incrementUse());
        assertEquals(1, coupon.getUseCount());
        
        assertTrue(coupon.incrementUse());
        assertEquals(2, coupon.getUseCount());
        
        assertFalse(coupon.incrementUse());
        assertEquals(3, coupon.getUseCount());
    }

    @Test
    @Order(4)
    @DisplayName("Test Coupon invalidation")
    void testCouponInvalidation() {
        SmartCouponSystem.Coupon coupon = new SmartCouponSystem.Coupon(
            "TESTCODE123", "campaign1", "user1", 
            LocalDateTime.now().plusDays(30), 5
        );
        
        assertTrue(coupon.isValid());
        coupon.invalidate();
        assertFalse(coupon.isValid());
    }

    // ===== CAMPAIGN CLASS TESTS =====

    @Test
    @Order(5)
    @DisplayName("Test CouponCampaign creation with valid parameters")
    void testCampaignCreation() {
        SmartCouponSystem.CouponCampaign campaign = new SmartCouponSystem.CouponCampaign(
            "SUMMER2025",
            SmartCouponSystem.UserSegment.RETURNING_USER,
            "ELECTRONICS",
            100.0,
            LocalDateTime.now().plusDays(30),
            3,
            "SUMMER"
        );
        
        assertEquals("SUMMER2025", campaign.getId());
        assertEquals(SmartCouponSystem.UserSegment.RETURNING_USER, campaign.getUserSegment());
        assertEquals("ELECTRONICS", campaign.getCategoryFilter());
        assertEquals(100.0, campaign.getMinCartValue());
        assertEquals(3, campaign.getMaxUsesPerUser());
        assertEquals("SUMMER", campaign.getPrefixPattern());
    }

    @Test
    @Order(6)
    @DisplayName("Test CouponCampaign creation with invalid parameters")
    void testCampaignCreationInvalidParameters() {
        // Null ID
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.CouponCampaign(null, SmartCouponSystem.UserSegment.ALL_USERS, 
                "ELECTRONICS", 100.0, LocalDateTime.now().plusDays(1), 1, "PREFIX"));
        
        // Blank ID
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.CouponCampaign("", SmartCouponSystem.UserSegment.ALL_USERS, 
                "ELECTRONICS", 100.0, LocalDateTime.now().plusDays(1), 1, "PREFIX"));
        
        // Null user segment
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.CouponCampaign("CAMPAIGN1", null, 
                "ELECTRONICS", 100.0, LocalDateTime.now().plusDays(1), 1, "PREFIX"));
        
        // Negative min cart value
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.CouponCampaign("CAMPAIGN1", SmartCouponSystem.UserSegment.ALL_USERS, 
                "ELECTRONICS", -1.0, LocalDateTime.now().plusDays(1), 1, "PREFIX"));
        
        // Null expiration
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.CouponCampaign("CAMPAIGN1", SmartCouponSystem.UserSegment.ALL_USERS, 
                "ELECTRONICS", 100.0, null, 1, "PREFIX"));
        
        // Zero max uses per user
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.CouponCampaign("CAMPAIGN1", SmartCouponSystem.UserSegment.ALL_USERS, 
                "ELECTRONICS", 100.0, LocalDateTime.now().plusDays(1), 0, "PREFIX"));
    }

    // ===== USER CLASS TESTS =====

    @Test
    @Order(7)
    @DisplayName("Test User creation with valid parameters")
    void testUserCreation() {
        SmartCouponSystem.User user = new SmartCouponSystem.User("user123", false, 10, 500.0);
        
        assertEquals("user123", user.getId());
        assertFalse(user.isNew());
        assertEquals(10, user.getPastOrderCount());
        assertEquals(500.0, user.getTotalSpent());
        assertTrue(user.getRedeemedCoupons().isEmpty());
    }

    @Test
    @Order(8)
    @DisplayName("Test User creation with invalid parameters")
    void testUserCreationInvalidParameters() {
        // Null ID
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.User(null, false, 5, 100.0));
        
        // Blank ID
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.User("", false, 5, 100.0));
        
        // Negative past order count
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.User("user123", false, -1, 100.0));
        
        // Negative total spent
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.User("user123", false, 5, -1.0));
    }

    @Test
    @Order(9)
    @DisplayName("Test User addRedeemedCoupon functionality")
    void testUserAddRedeemedCoupon() {
        SmartCouponSystem.User user = new SmartCouponSystem.User("user123", false, 10, 500.0);
        
        user.addRedeemedCoupon("COUPON123");
        assertTrue(user.getRedeemedCoupons().contains("COUPON123"));
        
        user.addRedeemedCoupon("COUPON456");
        assertTrue(user.getRedeemedCoupons().contains("COUPON456"));
        assertEquals(2, user.getRedeemedCoupons().size());
        
        // Test invalid coupon code
        assertThrows(IllegalArgumentException.class, () -> user.addRedeemedCoupon(null));
        assertThrows(IllegalArgumentException.class, () -> user.addRedeemedCoupon(""));
    }

    // ===== CART CLASS TESTS =====

    @Test
    @Order(10)
    @DisplayName("Test Cart creation with valid parameters")
    void testCartCreation() {
        Map<String, Double> items = new HashMap<>();
        items.put("LAPTOP", 1000.0);
        items.put("MOUSE", 50.0);
        
        SmartCouponSystem.Cart cart = new SmartCouponSystem.Cart("user123", items);
        
        assertEquals("user123", cart.getUserId());
        assertEquals(1050.0, cart.getTotalValue());
        assertEquals(2, cart.getItems().size());
        assertNotNull(cart.getLastUpdated());
    }

    @Test
    @Order(11)
    @DisplayName("Test Cart creation with invalid parameters")
    void testCartCreationInvalidParameters() {
        Map<String, Double> items = new HashMap<>();
        items.put("LAPTOP", 1000.0);
        
        // Null user ID
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.Cart(null, items));
        
        // Blank user ID
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.Cart("", items));
        
        // Null items
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.Cart("user123", null));
        
        // Negative price
        Map<String, Double> invalidItems = new HashMap<>();
        invalidItems.put("LAPTOP", -100.0);
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.Cart("user123", invalidItems));
    }

    @Test
    @Order(12)
    @DisplayName("Test Cart immutability")
    void testCartImmutability() {
        Map<String, Double> items = new HashMap<>();
        items.put("LAPTOP", 1000.0);
        
        SmartCouponSystem.Cart cart = new SmartCouponSystem.Cart("user123", items);
        Map<String, Double> cartItems = cart.getItems();
        
        // Should not be able to modify the returned map
        assertThrows(UnsupportedOperationException.class, () -> 
            cartItems.put("MOUSE", 50.0));
    }

    // ===== FRAUD ATTEMPT CLASS TESTS =====

    @Test
    @Order(13)
    @DisplayName("Test FraudAttempt creation with valid parameters")
    void testFraudAttemptCreation() {
        SmartCouponSystem.FraudAttempt attempt = new SmartCouponSystem.FraudAttempt(
            "COUPON123", "user123", "192.168.1.1", "device-fingerprint", false
        );
        
        assertEquals("COUPON123", attempt.getCouponCode());
        assertEquals("user123", attempt.getUserId());
        assertEquals("192.168.1.1", attempt.getIpAddress());
        assertEquals("device-fingerprint", attempt.getDeviceFingerprint());
        assertFalse(attempt.isSuccessful());
        assertNotNull(attempt.getTimestamp());
    }

    @Test
    @Order(14)
    @DisplayName("Test FraudAttempt creation with invalid parameters")
    void testFraudAttemptCreationInvalidParameters() {
        // Null coupon code
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.FraudAttempt(null, "user123", "192.168.1.1", "device", false));
        
        // Blank user ID
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.FraudAttempt("COUPON123", "", "192.168.1.1", "device", false));
        
        // Null IP address
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.FraudAttempt("COUPON123", "user123", null, "device", false));
        
        // Blank device fingerprint
        assertThrows(IllegalArgumentException.class, () -> 
            new SmartCouponSystem.FraudAttempt("COUPON123", "user123", "192.168.1.1", "", false));
    }

    // ===== SYSTEM LEVEL TESTS =====

    @Test
    @Order(15)
    @DisplayName("Test createCampaign functionality")
    void testCreateCampaign() {
        SmartCouponSystem.CouponCampaign newCampaign = new SmartCouponSystem.CouponCampaign(
            "NEW_CAMPAIGN",
            SmartCouponSystem.UserSegment.NEW_USER,
            "BOOKS",
            50.0,
            LocalDateTime.now().plusDays(30),
            2,
            "BOOK"
        );
        
        assertDoesNotThrow(() -> system.createCampaign(newCampaign));
        
        // Test duplicate campaign
        assertThrows(IllegalArgumentException.class, () -> system.createCampaign(newCampaign));
        
        // Test null campaign
        assertThrows(IllegalArgumentException.class, () -> system.createCampaign(null));
    }

    @Test
    @Order(16)
    @DisplayName("Test generateCoupon functionality")
    void testGenerateCoupon() {
        SmartCouponSystem.Coupon coupon = system.generateCoupon("TEST_CAMPAIGN", "user123");
        
        assertNotNull(coupon);
        assertTrue(coupon.getCode().startsWith("TEST-"));
        assertEquals("TEST_CAMPAIGN", coupon.getCampaignId());
        assertEquals("user123", coupon.getUserId());
        
        // Test invalid parameters
        assertThrows(IllegalArgumentException.class, () -> 
            system.generateCoupon(null, "user123"));
        assertThrows(IllegalArgumentException.class, () -> 
            system.generateCoupon("TEST_CAMPAIGN", ""));
        
        // Test non-existent campaign
        assertNull(system.generateCoupon("NON_EXISTENT", "user123"));
    }

    @Test
    @Order(17)
    @DisplayName("Test validateAndRedeem successful redemption")
    void testValidateAndRedeemSuccess() {
        SmartCouponSystem.Coupon coupon = system.generateCoupon("TEST_CAMPAIGN", "user123");
        
        boolean result = system.validateAndRedeem(
            coupon.getCode(), testCart, "192.168.1.1", "device-fingerprint-123"
        );
        
        assertTrue(result);
        assertEquals(1, coupon.getUseCount());
        assertTrue(testUser.getRedeemedCoupons().contains(coupon.getCode()));
    }

    @Test
    @Order(18)
    @DisplayName("Test validateAndRedeem with invalid parameters")
    void testValidateAndRedeemInvalidParameters() {
        SmartCouponSystem.Coupon coupon = system.generateCoupon("TEST_CAMPAIGN", "user123");
        
        // Null coupon code
        assertThrows(IllegalArgumentException.class, () -> 
            system.validateAndRedeem(null, testCart, "192.168.1.1", "device"));
        
        // Null cart
        assertThrows(IllegalArgumentException.class, () -> 
            system.validateAndRedeem(coupon.getCode(), null, "192.168.1.1", "device"));
        
        // Blank IP address
        assertThrows(IllegalArgumentException.class, () -> 
            system.validateAndRedeem(coupon.getCode(), testCart, "", "device"));
        
        // Null device fingerprint
        assertThrows(IllegalArgumentException.class, () -> 
            system.validateAndRedeem(coupon.getCode(), testCart, "192.168.1.1", null));
    }

    @Test
    @Order(19)
    @DisplayName("Test validateAndRedeem with non-existent coupon")
    void testValidateAndRedeemNonExistentCoupon() {
        boolean result = system.validateAndRedeem(
            "NON_EXISTENT", testCart, "192.168.1.1", "device-fingerprint-123"
        );
        
        assertFalse(result);
    }

    @Test
    @Order(20)
    @DisplayName("Test validateAndRedeem with expired coupon")
    void testValidateAndRedeemExpiredCoupon() {
        SmartCouponSystem.Coupon expiredCoupon = new SmartCouponSystem.Coupon(
            "EXPIRED", "TEST_CAMPAIGN", "user123", 
            LocalDateTime.now().minusDays(1), 1
        );
        system.addCouponForTesting(expiredCoupon);
        
        boolean result = system.validateAndRedeem(
            "EXPIRED", testCart, "192.168.1.1", "device-fingerprint-123"
        );
        
        assertFalse(result);
    }

    @Test
    @Order(21)
    @DisplayName("Test validateAndRedeem with insufficient cart value")
    void testValidateAndRedeemInsufficientCartValue() {
        // Create campaign with high minimum cart value
        SmartCouponSystem.CouponCampaign highValueCampaign = new SmartCouponSystem.CouponCampaign(
            "HIGH_VALUE",
            SmartCouponSystem.UserSegment.ALL_USERS,
            "ELECTRONICS",
            5000.0, // High minimum
            LocalDateTime.now().plusDays(30),
            3,
            "HIGH"
        );
        system.createCampaign(highValueCampaign);
        
        SmartCouponSystem.Coupon coupon = system.generateCoupon("HIGH_VALUE", "user123");
        
        boolean result = system.validateAndRedeem(
            coupon.getCode(), testCart, "192.168.1.1", "device-fingerprint-123"
        );
        
        assertFalse(result);
    }

    @Test
    @Order(22)
    @DisplayName("Test validateAndRedeem with wrong user segment")
    void testValidateAndRedeemWrongUserSegment() {
        // Create campaign for new users only
        SmartCouponSystem.CouponCampaign newUserCampaign = new SmartCouponSystem.CouponCampaign(
            "NEW_USER_ONLY",
            SmartCouponSystem.UserSegment.NEW_USER,
            "ELECTRONICS",
            100.0,
            LocalDateTime.now().plusDays(30),
            3,
            "NEW"
        );
        system.createCampaign(newUserCampaign);
        
        SmartCouponSystem.Coupon coupon = system.generateCoupon("NEW_USER_ONLY", "user123");
        
        boolean result = system.validateAndRedeem(
            coupon.getCode(), testCart, "192.168.1.1", "device-fingerprint-123"
        );
        
        assertFalse(result); // testUser is not new
    }

    @Test
    @Order(23)
    @DisplayName("Test fraud detection and lockout")
    void testFraudDetectionAndLockout() {
        String ipAddress = "192.168.1.100";
        String deviceFingerprint = "device-fraud-test";
        
        // Make 5 failed attempts
        for (int i = 0; i < 5; i++) {
            system.validateAndRedeem(
                "NON_EXISTENT_" + i, testCart, ipAddress, deviceFingerprint
            );
        }
        
        // User should now be locked out
        assertTrue(system.isUserLockedOut("user123", ipAddress));
        
        // Valid coupon should also fail due to lockout
        SmartCouponSystem.Coupon validCoupon = system.generateCoupon("TEST_CAMPAIGN", "user123");
        boolean result = system.validateAndRedeem(
            validCoupon.getCode(), testCart, ipAddress, deviceFingerprint
        );
        
        assertFalse(result);
    }

    @Test
    @Order(24)
    @DisplayName("Test isUserLockedOut with invalid parameters")
    void testIsUserLockedOutInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> 
            system.isUserLockedOut(null, "192.168.1.1"));
        assertThrows(IllegalArgumentException.class, () -> 
            system.isUserLockedOut("user123", ""));
    }

    @Test
    @Order(25)
    @DisplayName("Test generatePersonalizedCoupons")
    void testGeneratePersonalizedCoupons() {
        // Add abandoned cart
        system.addAbandonedCart(testCart);
        
        // Generate personalized coupons
        List<SmartCouponSystem.Coupon> coupons = system.generatePersonalizedCoupons(testUser);
        
        assertNotNull(coupons);
        assertFalse(coupons.isEmpty()); // testUser qualifies (pastOrderCount >= 3)
        
        // Test with new user (should not qualify)
        List<SmartCouponSystem.Coupon> newUserCoupons = system.generatePersonalizedCoupons(newUser);
        assertTrue(newUserCoupons.isEmpty());
        
        // Test with null user
        assertThrows(IllegalArgumentException.class, () -> 
            system.generatePersonalizedCoupons(null));
    }

    @Test
    @Order(26)
    @DisplayName("Test getActiveCampaigns")
    void testGetActiveCampaigns() {
        List<SmartCouponSystem.CouponCampaign> activeCampaigns = system.getActiveCampaigns();
        
        assertNotNull(activeCampaigns);
        assertFalse(activeCampaigns.isEmpty());
        
        // All returned campaigns should be active
        LocalDateTime now = LocalDateTime.now();
        for (SmartCouponSystem.CouponCampaign campaign : activeCampaigns) {
            assertTrue(campaign.getExpiration().isAfter(now));
        }
    }

    @Test
    @Order(27)
    @DisplayName("Test getExpiredCampaigns")
    void testGetExpiredCampaigns() {
        // Create an expired campaign
        SmartCouponSystem.CouponCampaign expiredCampaign = new SmartCouponSystem.CouponCampaign(
            "EXPIRED_CAMPAIGN",
            SmartCouponSystem.UserSegment.ALL_USERS,
            "BOOKS",
            50.0,
            LocalDateTime.now().minusDays(1), // Expired
            3,
            "EXPIRED"
        );
        system.createCampaign(expiredCampaign);
        
        List<SmartCouponSystem.CouponCampaign> expiredCampaigns = system.getExpiredCampaigns();
        
        assertNotNull(expiredCampaigns);
        assertFalse(expiredCampaigns.isEmpty());
        
        // All returned campaigns should be expired
        LocalDateTime now = LocalDateTime.now();
        for (SmartCouponSystem.CouponCampaign campaign : expiredCampaigns) {
            assertTrue(campaign.getExpiration().isBefore(now));
        }
    }

    @Test
    @Order(28)
    @DisplayName("Test getTopRedeemedCoupons")
    void testGetTopRedeemedCoupons() {
        // Create and redeem some coupons
        SmartCouponSystem.Coupon coupon1 = system.generateCoupon("TEST_CAMPAIGN", "user123");
        SmartCouponSystem.Coupon coupon2 = system.generateCoupon("TEST_CAMPAIGN", "user123");
        
        // Redeem coupon1 twice
        system.validateAndRedeem(coupon1.getCode(), testCart, "192.168.1.10", "device1");
        system.validateAndRedeem(coupon1.getCode(), testCart, "192.168.1.11", "device2");
        
        // Redeem coupon2 once
        system.validateAndRedeem(coupon2.getCode(), testCart, "192.168.1.12", "device3");
        
        List<SmartCouponSystem.Coupon> topCoupons = system.getTopRedeemedCoupons(5);
        
        assertNotNull(topCoupons);
        assertFalse(topCoupons.isEmpty());
        
        // Should be sorted by use count (descending)
        if (topCoupons.size() >= 2) {
            assertTrue(topCoupons.get(0).getUseCount() >= topCoupons.get(1).getUseCount());
        }
        
        // Test invalid limit
        assertThrows(IllegalArgumentException.class, () -> 
            system.getTopRedeemedCoupons(0));
        assertThrows(IllegalArgumentException.class, () -> 
            system.getTopRedeemedCoupons(-1));
    }

    @Test
    @Order(29)
    @DisplayName("Test getFlaggedFraudEvents")
    void testGetFlaggedFraudEvents() {
        // Make some failed attempts
        system.validateAndRedeem("INVALID1", testCart, "192.168.1.50", "device-fraud");
        system.validateAndRedeem("INVALID2", testCart, "192.168.1.51", "device-fraud");
        
        List<SmartCouponSystem.FraudAttempt> fraudEvents = system.getFlaggedFraudEvents();
        
        assertNotNull(fraudEvents);
        assertFalse(fraudEvents.isEmpty());
        
        // All returned events should be failed attempts
        for (SmartCouponSystem.FraudAttempt attempt : fraudEvents) {
            assertFalse(attempt.isSuccessful());
        }
    }

    @Test
    @Order(30)
    @DisplayName("Test addUser functionality")
    void testAddUser() {
        SmartCouponSystem.User newSystemUser = new SmartCouponSystem.User("newUser789", true, 0, 0.0);
        
        assertDoesNotThrow(() -> system.addUser(newSystemUser));
        
        // Test duplicate user
        assertThrows(IllegalArgumentException.class, () -> system.addUser(newSystemUser));
        
        // Test null user
        assertThrows(IllegalArgumentException.class, () -> system.addUser(null));
    }

    @Test
    @Order(31)
    @DisplayName("Test addAbandonedCart functionality")
    void testAddAbandonedCart() {
        Map<String, Double> cartItems = new HashMap<>();
        cartItems.put("BOOK-NOVEL", 25.0);
        SmartCouponSystem.Cart newCart = new SmartCouponSystem.Cart("newUser456", cartItems);
        
        assertDoesNotThrow(() -> system.addAbandonedCart(newCart));
        
        // Test duplicate cart for same user
        SmartCouponSystem.Cart duplicateCart = new SmartCouponSystem.Cart("newUser456", cartItems);
        assertThrows(IllegalArgumentException.class, () -> system.addAbandonedCart(duplicateCart));
        
        // Test null cart
        assertThrows(IllegalArgumentException.class, () -> system.addAbandonedCart(null));
    }

    @Test
    @Order(32)
    @DisplayName("Test addCouponForTesting functionality")
    void testAddCouponForTesting() {
        SmartCouponSystem.Coupon testCoupon = new SmartCouponSystem.Coupon(
            "MANUAL_TEST", "TEST_CAMPAIGN", "user123", 
            LocalDateTime.now().plusDays(30), 1
        );
        
        assertDoesNotThrow(() -> system.addCouponForTesting(testCoupon));
        
        // Test duplicate coupon
        SmartCouponSystem.Coupon duplicateCoupon = new SmartCouponSystem.Coupon(
            "MANUAL_TEST", "TEST_CAMPAIGN", "user123", 
            LocalDateTime.now().plusDays(30), 1
        );
        assertThrows(IllegalArgumentException.class, () -> system.addCouponForTesting(duplicateCoupon));
        
        // Test null coupon
        assertThrows(IllegalArgumentException.class, () -> system.addCouponForTesting(null));
    }

    // ===== CONCURRENCY TESTS =====

    @Test
    @Order(33)
    @DisplayName("Test concurrent coupon redemption")
    @Execution(ExecutionMode.CONCURRENT)
    void testConcurrentCouponRedemption() throws InterruptedException {
        SmartCouponSystem.Coupon coupon = system.generateCoupon("TEST_CAMPAIGN", "user123");
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    boolean success = system.validateAndRedeem(
                        coupon.getCode(), testCart, "192.168.1." + threadId, "device-" + threadId
                    );
                    if (success) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await(5, TimeUnit.SECONDS);
        
        // Only max uses should succeed
        assertTrue(successCount.get() <= coupon.getMaxUses());
    }

    // ===== EDGE CASE TESTS =====

    @Test
    @Order(34)
    @DisplayName("Test edge case - zero cart value")
    void testZeroCartValue() {
        Map<String, Double> emptyCartItems = new HashMap<>();
        SmartCouponSystem.Cart emptyCart = new SmartCouponSystem.Cart("user123", emptyCartItems);
        
        assertEquals(0.0, emptyCart.getTotalValue());
        
        SmartCouponSystem.Coupon coupon = system.generateCoupon("TEST_CAMPAIGN", "user123");
        boolean result = system.validateAndRedeem(
            coupon.getCode(), emptyCart, "192.168.1.1", "device"
        );
        
        assertFalse(result); // Should fail due to min cart value requirement
    }

    @Test
    @Order(35)
    @DisplayName("Test edge case - boundary cart value")
    void testBoundaryCartValue() {
        // Create cart with exactly minimum value
        Map<String, Double> boundaryItems = new HashMap<>();
        boundaryItems.put("ITEM1", 100.0); // Exactly the minimum cart value
        SmartCouponSystem.Cart boundaryCart = new SmartCouponSystem.Cart("user123", boundaryItems);
        
        SmartCouponSystem.Coupon coupon = system.generateCoupon("TEST_CAMPAIGN", "user123");
        boolean result = system.validateAndRedeem(
            coupon.getCode(), boundaryCart, "192.168.1.1", "device"
        );
        
        assertTrue(result); // Should succeed as it meets minimum requirement
    }

    @Test
    @Order(36)
    @DisplayName("Test user segment matching")
    void testUserSegmentMatching() {
        // Test NEW_USER segment
        SmartCouponSystem.CouponCampaign newUserCampaign = new SmartCouponSystem.CouponCampaign(
            "NEW_USERS_ONLY",
            SmartCouponSystem.UserSegment.NEW_USER,
            "ELECTRONICS",
            100.0,
            LocalDateTime.now().plusDays(30),
            3,
            "NEW"
        );
        system.createCampaign(newUserCampaign);
        
        SmartCouponSystem.Coupon newUserCoupon = system.generateCoupon("NEW_USERS_ONLY", "newUser456");
        
        Map<String, Double> newUserCartItems = new HashMap<>();
        newUserCartItems.put("ELECTRONICS-TABLET", 500.0);
        SmartCouponSystem.Cart newUserCart = new SmartCouponSystem.Cart("newUser456", newUserCartItems);
        
        boolean result = system.validateAndRedeem(
            newUserCoupon.getCode(), newUserCart, "192.168.1.1", "device"
        );
        
        assertTrue(result); // Should succeed for new user
        
        // Test RETURNING_USER segment
        SmartCouponSystem.CouponCampaign returningUserCampaign = new SmartCouponSystem.CouponCampaign(
            "RETURNING_USERS_ONLY",
            SmartCouponSystem.UserSegment.RETURNING_USER,
            "ELECTRONICS",
            100.0,
            LocalDateTime.now().plusDays(30),
            3,
            "RETURN"
        );
        system.createCampaign(returningUserCampaign);
        
        SmartCouponSystem.Coupon returningUserCoupon = system.generateCoupon("RETURNING_USERS_ONLY", "user123");
        
        boolean returningResult = system.validateAndRedeem(
            returningUserCoupon.getCode(), testCart, "192.168.1.2", "device2"
        );
        
        assertTrue(returningResult); // Should succeed for returning user
    }

    @AfterEach
    void tearDown() {
        // Clean up resources if needed
        system = null;
        testUser = null;
        newUser = null;
        testCampaign = null;
        testCart = null;
    }
} 
