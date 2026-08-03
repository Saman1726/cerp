package io.cerp.workflow.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.cerp.workflow.event.WorkflowEvent;
import io.cerp.workflow.service.EventPublisher;
import io.cerp.workflow.service.bidding.BidWalletService;
import io.cerp.workflow.service.bidding.VendorBidSubmissionService;
import io.cerp.workflow.domain.bidding.VendorBidSubmission;

class BiddingSagaHandlerTest {

    private BiddingSagaHandler handler;
    private MockEventPublisher eventPublisher;
    private VendorBidSubmissionService bidSubmissionService;
    private BidWalletService bidWalletService;

    @BeforeEach
    void setUp() {
        eventPublisher = new MockEventPublisher();
        bidSubmissionService = mock(VendorBidSubmissionService.class);
        bidWalletService = mock(BidWalletService.class);
        handler = new BiddingSagaHandler(eventPublisher, bidSubmissionService, bidWalletService);
    }

    @Test
    void testGetStepCode() {
        assertEquals("Tender.CreateAndOpen", handler.getStepCode());
    }

    @Test
    void testExecuteCreateTender() throws Exception {
        final UUID sagaId = UUID.randomUUID();
        final SagaContext context = new SagaContext(sagaId, "marketplace_bidding", "tenant-1", new HashMap<>());
        final Map<String, Object> payload = new HashMap<>();
        payload.put("projectId", "proj-123");

        final SagaContext result = handler.execute(context, "Tender.CreateAndOpen", payload);

        assertNotNull(result);
        assertEquals("proj-123", result.getData().get("projectId"));
        assertTrue(result.getData().containsKey("tenderId"));
        assertEquals("OPEN", result.getData().get("status"));
        assertEquals(1, eventPublisher.getPublishedEvents().size());
        assertEquals("TenderCreated", eventPublisher.getPublishedEvents().get(0).getEventType());
    }

    @Test
    void testExecuteCreateTenderWithoutProjectId() {
        final UUID sagaId = UUID.randomUUID();
        final SagaContext context = new SagaContext(sagaId, "marketplace_bidding", "tenant-1", new HashMap<>());
        final Map<String, Object> payload = new HashMap<>();

        final SagaException ex = assertThrows(SagaException.class,
            () -> handler.execute(context, "Tender.CreateAndOpen", payload));
        assertEquals("VALIDATION_ERROR", ex.getErrorCode());
        assertEquals(0, eventPublisher.getPublishedEvents().size());
    }

    @Test
    void testExecuteInviteVendors() throws Exception {
        final UUID sagaId = UUID.randomUUID();
        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("tenderId", "tender-1");
        final SagaContext context = new SagaContext(sagaId, "marketplace_bidding", "tenant-1", initialData);

        final Map<String, Object> payload = new HashMap<>();
        payload.put("vendorIds", List.of("vendor-1", "vendor-2"));
        final Map<String, Object> inviteConfig = new HashMap<>();
        inviteConfig.put("expectedBids", 2);
        inviteConfig.put("bidDeadline", "2026-08-01T00:00:00Z");
        payload.put("inviteConfig", inviteConfig);

        final SagaContext result = handler.execute(context, "Tender.InviteVendors", payload);

        assertNotNull(result);
        assertEquals(2, ((List<?>) result.getData().get("invitedVendorIds")).size());
        assertEquals("UNDER_EVALUATION", result.getData().get("status"));
        assertEquals(2, ((Number) result.getData().get("expectedBids")).intValue());
        assertEquals("2026-08-01T00:00:00Z", result.getData().get("bidDeadline"));
        assertEquals(1, eventPublisher.getPublishedEvents().size());
        assertEquals("VendorsInvited", eventPublisher.getPublishedEvents().get(0).getEventType());
    }

    @Test
    void testExecuteCollectBids() throws Exception {
        final UUID sagaId = UUID.randomUUID();
        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("tenderId", "tender-1");
        initialData.put("biddingSagaId", sagaId.toString());
        final SagaContext context = new SagaContext(sagaId, "marketplace_bidding", "tenant-1", initialData);

        // Create mock bid submissions
        VendorBidSubmission bid1 = createMockBid(sagaId, "vendor-1", 100.0);
        VendorBidSubmission bid2 = createMockBid(sagaId, "vendor-2", 90.0);
        when(bidSubmissionService.getBidsBySagaId(sagaId)).thenReturn(List.of(bid1, bid2));

        final SagaContext result = handler.execute(context, "Bidding.CollectBids", null);

        assertNotNull(result);
        assertEquals(2, ((Number) result.getData().get("bidsReceived")).intValue());
        assertTrue(result.getData().containsKey("bids"));
        assertEquals(2, ((List<?>) result.getData().get("bids")).size());
        assertEquals(1, eventPublisher.getPublishedEvents().size());
        assertEquals("BidsCollected", eventPublisher.getPublishedEvents().get(0).getEventType());
    }

    @Test
    void testExecuteCollectBidsEmptyThrows() {
        final UUID sagaId = UUID.randomUUID();
        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("tenderId", "tender-1");
        initialData.put("biddingSagaId", sagaId.toString());
        final SagaContext context = new SagaContext(sagaId, "marketplace_bidding", "tenant-1", initialData);

        // No bids - should throw NO_BIDS_RECEIVED
        when(bidSubmissionService.getBidsBySagaId(sagaId)).thenReturn(List.of());

        final SagaException ex = assertThrows(SagaException.class,
            () -> handler.execute(context, "Bidding.CollectBids", null));
        assertEquals("NO_BIDS_RECEIVED", ex.getErrorCode());
        assertEquals(0, eventPublisher.getPublishedEvents().size());
    }

    @Test
    void testExecuteEvaluateBids() throws Exception {
        final UUID sagaId = UUID.randomUUID();
        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("tenderId", "tender-1");
        final Map<String, Object> bid1 = Map.of("vendorId", "vendor-1", "price", 100.0);
        final Map<String, Object> bid2 = Map.of("vendorId", "vendor-2", "price", 90.0);
        initialData.put("bids", List.of(bid1, bid2));
        final SagaContext context = new SagaContext(sagaId, "marketplace_bidding", "tenant-1", initialData);

        final SagaContext result = handler.execute(context, "Bidding.Evaluate", null);

        assertNotNull(result);
        assertEquals("vendor-2", result.getData().get("winningVendorId"));
        assertEquals(90.0, ((Number) result.getData().get("winningPrice")).doubleValue());
        assertEquals(1, eventPublisher.getPublishedEvents().size());
        assertEquals("TenderEvaluated", eventPublisher.getPublishedEvents().get(0).getEventType());
    }

    @Test
    void testExecuteAwardTender() throws Exception {
        final UUID sagaId = UUID.randomUUID();
        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("tenderId", "tender-1");
        initialData.put("winningVendorId", "vendor-2");
        initialData.put("winningPrice", 90.0);
        final SagaContext context = new SagaContext(sagaId, "marketplace_bidding", "tenant-1", initialData);

        final SagaContext result = handler.execute(context, "Tender.Award", null);

        assertNotNull(result);
        assertTrue((Boolean) result.getData().get("poIssued"));
        assertEquals("AWARDED", result.getData().get("status"));
        assertEquals(2, eventPublisher.getPublishedEvents().size());
        assertEquals("TenderAwarded", eventPublisher.getPublishedEvents().get(0).getEventType());
        assertEquals("POIssued", eventPublisher.getPublishedEvents().get(1).getEventType());
    }

    @Test
    void testGetNextStep() throws Exception {
        final UUID sagaId = UUID.randomUUID();
        final SagaContext context = new SagaContext(sagaId, "marketplace_bidding", "tenant-1", new HashMap<>());

        String next = handler.getNextStep(context, "Tender.CreateAndOpen");
        assertEquals("Tender.InviteVendors", next);

        next = handler.getNextStep(context, "Tender.InviteVendors");
        assertNull(next); // saga pauses here; ResumeSaga with stepCode=Bidding.CollectBids advances it

        next = handler.getNextStep(context, "Bidding.CollectBids");
        assertEquals("Bidding.Evaluate", next);

        next = handler.getNextStep(context, "Bidding.Evaluate");
        assertEquals("Tender.Award", next);

        next = handler.getNextStep(context, "Tender.Award");
        assertTrue(next == null);
    }

    @Test
    void testCompensateAwardTender() throws Exception {
        final UUID sagaId = UUID.randomUUID();
        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("tenderId", "tender-1");
        final SagaContext context = new SagaContext(sagaId, "marketplace_bidding", "tenant-1", initialData);

        final SagaContext result = handler.compensate(context, "Tender.Award");
        assertNotNull(result);
        assertEquals(1, eventPublisher.getPublishedEvents().size());
        assertEquals("POCancelled", eventPublisher.getPublishedEvents().get(0).getEventType());
    }

    @Test
    void testCompensateAllSteps() throws Exception {
        final UUID sagaId = UUID.randomUUID();
        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("tenderId", "tender-1");
        initialData.put("invitedVendorIds", List.of("vendor-1"));
        final SagaContext context = new SagaContext(sagaId, "marketplace_bidding", "tenant-1", initialData);

        handler.compensate(context, "Tender.Award");
        handler.compensate(context, "Bidding.Evaluate");
        handler.compensate(context, "Bidding.CollectBids");
        handler.compensate(context, "Tender.InviteVendors");
        handler.compensate(context, "Tender.CreateAndOpen");

        assertEquals(5, eventPublisher.getPublishedEvents().size());
    }

    // ── Helpers ──

    private VendorBidSubmission createMockBid(UUID sagaId, String vendorId, double price) {
        UUID bidId = UUID.randomUUID();
        VendorBidSubmission bid = new VendorBidSubmission(
            "tender-1",
            sagaId,
            UUID.randomUUID(), // vendorId
            UUID.randomUUID(), // vendorUserId
            UUID.randomUUID(),
            new BigDecimal(price),
            BigDecimal.ONE,
            new BigDecimal(price),
            new BigDecimal(price),
            "30 days",
            null
        );
        // Set IDs and timestamps manually (these are normally set by JPA)
        bid.setId(bidId);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        bid.setCreatedAt(now);
        bid.setUpdatedAt(now);
        bid.setSubmittedAt(now);
        return bid;
    }

    /**
     * Simple mock event publisher for testing without Spring dependencies.
     */
    private static class MockEventPublisher implements EventPublisher {
        private final List<WorkflowEvent> publishedEvents = new ArrayList<>();

        @Override
        public void publish(final WorkflowEvent event) {
            publishedEvents.add(event);
        }

        @Override
        public void publish(final WorkflowEvent event, final Map<String, Object> metadata) {
            publishedEvents.add(event);
        }

        @Override
        public void publishEvent(final String eventType, final String sagaType, final UUID aggregateId,
                final String tenantId, final Map<String, Object> data) {
            // Not implemented for testing
        }

        @Override
        public void publishEvent(final String eventType, final String sagaType, final String sagaId,
                final UUID aggregateId, final String tenantId, final Map<String, Object> data) {
            // Not implemented for testing
        }

        @Override
        public void publishEvent(final String eventType, final String sagaType, final String sagaId,
                final UUID aggregateId, final String tenantId, final String userId, final Map<String, Object> data) {
            // Not implemented for testing
        }

        @Override
        public boolean isHealthy() {
            return true;
        }

        public List<WorkflowEvent> getPublishedEvents() {
            return java.util.Collections.unmodifiableList(publishedEvents);
        }

        public void clear() {
            publishedEvents.clear();
        }
    }
}