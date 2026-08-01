package io.cerp.workflow.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.cerp.workflow.event.WorkflowEvent;
import io.cerp.workflow.service.EventPublisher;

class BiddingSagaHandlerTest {

    private BiddingSagaHandler handler;
    private MockEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = new MockEventPublisher();
        handler = new BiddingSagaHandler(eventPublisher);
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
        // expectedBids is an Integer
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
        final SagaContext context = new SagaContext(sagaId, "marketplace_bidding", "tenant-1", initialData);

        final Map<String, Object> payload = new HashMap<>();
        final Map<String, Object> bid1 = Map.of("vendorId", "vendor-1", "price", 100.0);
        final Map<String, Object> bid2 = Map.of("vendorId", "vendor-2", "price", 90.0);
        payload.put("bids", List.of(bid1, bid2));

        final SagaContext result = handler.execute(context, "Bidding.CollectBids", payload);

        assertNotNull(result);
        // bidsReceived is an integer count, not a list
        assertEquals(2, ((Number) result.getData().get("bidsReceived")).intValue());
        // bids is a List
        assertTrue(result.getData().containsKey("bids"));
        assertEquals(2, ((List<?>) result.getData().get("bids")).size());
        assertEquals(1, eventPublisher.getPublishedEvents().size());
        assertEquals("BidsCollected", eventPublisher.getPublishedEvents().get(0).getEventType());
    }

    @Test
    void testExecuteCollectBidsEmptyThrows() {
        final UUID sagaId = UUID.randomUUID();
        final SagaContext context = new SagaContext(sagaId, "marketplace_bidding", "tenant-1", new HashMap<>());
        final Map<String, Object> payload = new HashMap<>();
        payload.put("bids", List.of());

        final SagaException ex = assertThrows(SagaException.class,
            () -> handler.execute(context, "Bidding.CollectBids", payload));
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
        assertEquals("Bidding.CollectBids", next);

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
