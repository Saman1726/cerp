# Document Lifecycle Workflow

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Author
    participant Reviewer
    participant GW as API Gateway
    participant Doc as Document Service
    participant Id as Identity Service
    participant Fin as Financials Service

    %% Step 1: Document Creation
    Author->>GW: POST /documents {type, projectId, title, content}
    GW->>Doc: VerifyProjectAccess(tenantId, projectId, roles)
    GW->>Doc: CreateDocument
    Doc->>Doc: Validate(ValidType, ProjectExists, ContentPresent)
    Doc->>Doc: Emit DocumentCreated
    Doc-->>GW: 201 Created({documentId, status: "Draft"})

    %% Step 2: Submit for Review
    Author->>GW: POST /documents/{id}/submit-review
    GW->>Doc: SubmitForReview(documentId)
    Doc->>Doc: Validate(Draft, RequiredFields)
    Doc->>Doc: Emit DocumentSubmittedForReview
    Doc->>Doc: ScheduleReview(documentId, reviewWindow)
    Doc-->>GW: 200 OK

    Note over Doc: Review Period Starts

    alt Within Review Window
        %% Step 3: Review Outcome
        Reviewer->>GW: POST /documents/{id}/approve OR /documents/{id}/reject
        GW->>Doc: ProcessReviewOutcome(documentId, approved)
        Doc->>Doc: Emit DocumentApproved OR DocumentRejected
        alt Approved
            Doc->>Doc: SetVersion(documentId)
            Doc->>Doc: PublishToRepository(documentId)
            Doc->>Doc: Emit DocumentPublished
            Doc->>Fin: VersionRecorded(documentId, versionNumber)
        else Rejected
            Doc->>Doc: ReturnToDraft(documentId, notes)
        end
        Doc-->>GW: 200 OK
        GW-->>Reviewer: {status, version}
    else Review Window Expired
        Doc->>Doc: Emit ReviewExpired
        Doc->>Doc: AutoReject(documentId)
        Note over Doc: Auto-reject prevents stale reviews
    end
```

## Service Interaction Summary

| Interaction | From         | To               | Purpose                           |
| ----------- | ------------ | ---------------- | --------------------------------- |
| 1           | Author       | API Gateway      | Create document                   |
| 2           | API Gateway  | Document Service | Verify access and create document |
| 3           | Author       | API Gateway      | Submit document for review        |
| 4           | Reviewer     | API Gateway      | Approve or reject document        |
| 5           | Document Svc | Financials Svc   | Record version for versioning     |

## Document Type Hierarchy

| Type     | Subtype     | Description                     |
| -------- | ----------- | ------------------------------- |
| Contract | Legal, Misc | Legally binding agreements      |
| Invoice  | Tax         | Financial and tax documentation |
| PO       | Purchase    | Purchase order processing       |
| Spec     | Tech, Other | Technical specifications        |

## Versioning Rules

| Rule                    | Value                              |
| ----------------------- | ---------------------------------- |
| Max Versions (default)  | 99                                 |
| Max Versions (contract) | 999                                |
| New Version             | Published document + changes       |
| No Direct Editing       | Drafts only editable by author     |
| Rollback                | Reverts to prior published version |

## Event Propagation Chain

```
DocumentCreated
    → DocumentSubmittedForReview
        → ReviewExpired (if timeout)
        → DocumentApproved
        → DocumentRejected
        → DocumentPublished
            → VersionRecorded (Financials Service)
```

## File Information

- **Workflow**: Document Lifecycle
- **Complexity**: Medium (4 steps, 6 events, 4 services)
- **Phase Priority**: Phase 2
- **Related Workflows**: Project Management, Financial Close
