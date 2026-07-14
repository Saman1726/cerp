# ConstructHub – Business Concept & Software Architecture

## 1. Executive Summary

- **What is ConstructHub?** A comprehensive, multi-tenant Construction Management Platform designed to streamline and digitize the entire construction lifecycle.
- **Who is it for?** Various stakeholders in the construction industry, including Organization Administrators, Project Managers, Constructors, and Vendors.
- **What problems does it solve?** It addresses information silos, manual procurement, paper-based reporting, poor communication, lack of real-time visibility, and decentralized document management.
- **Why is it different?** Its modern architecture (DDD, CQRS, Event Sourcing) provides a scalable, resilient, and future-proof foundation for digital transformation, offering a centralized platform for collaboration and data-driven decisions.

## 2. Problem Statement

The construction industry currently faces significant challenges that hinder efficiency and profitability:

- Information is spread across multiple disconnected systems (email, spreadsheets, paper).
- Procurement of materials is often a manual, time-consuming process.
- Delivery reports are paper-based, leading to delays and data entry errors.
- Poor communication and coordination between contractors, subcontractors, and vendors.
- Lack of real-time visibility into project progress, costs, and inventory.
- No centralized system for managing critical documents like blueprints, contracts, and change orders.

## 3. Business Objectives

- Reduce procurement time and associated costs.
- Increase project transparency for all stakeholders.
- Improve collaboration between office and field teams.
- Reduce material waste through better inventory management.
- Digitize and automate core construction processes.
- Support a multi-tenant architecture for different organizations.

## 4. Stakeholders / User Personas

- **Organization Administrator:** Creates organizations, projects, and assigns Project Managers.
- **Project Manager:** Manages project scope, approves procurements, and monitors costs.
- **Constructor:** Updates field progress, receives deliveries, and requests materials.
- **Vendor:** Publishes material catalogs, receives procurement requests, and confirms deliveries.

## 5. Functional Modules

- **User Management:** User Preferences, Membership & Roles, Authorization (RBAC).
- **Organizations:** Management of tenant companies.
- **Projects:** Management of projects and sub-projects.
- **Vendor Management:** Onboarding and management of material suppliers.
- **Materials & Inventory:** Central catalog of materials and site-specific inventory.
- **Procurement:** Creation, approval, and tracking of material requests.
- **Delivery Management:** Scheduling and confirmation of material deliveries.
- **Financials:** Cost tracking and budget management.
- **Workflow & Approvals:** Configurable approval chains for key processes.
- **Notifications:** Real-time alerts for important events.
- **Subcontractor Progress:** Tracking of work completion.
- **Reviews:** Formal review and sign-off processes.
- **Document Management (DMS):** Centralized storage for all project documents.
- **Reporting & Analytics:** Dashboards and reports on project KPIs.
- **Marketplace & Bidding System:** Management of public and private tenders for materials and services, connecting constructors to suppliers and subcontractors.
- **AI Material Estimator:** An intelligent tool to forecast material quantities (e.g., steel, concrete) based on initial project parameters like structure type, area, and number of floors, aiding in preliminary budgeting and planning.

## 6. Business Processes (High-Level Workflows)

- **Procurement:** `Project Manager` -> Create Procurement Request -> `Approval` -> `Vendor` -> `Delivery` -> Inventory Updated -> Financials Updated.
- **Material Delivery:** `Vendor` -> Delivery Created -> `Site Confirms Delivery` -> Inventory Updated -> Financials Updated.
- **User Assignment:** `Organization` -> `Project` -> `SubProject` -> Assign User -> Assign Role -> Permissions Generated.

## 7. Domain Model

- **Core Aggregates:** Organization, Project, SubProject, Membership, User, Role, Permissions.

## 8. Software Architecture

- **Domain-Driven Design (DDD):** The system is modeled around business domains with clear bounded contexts.
- **CQRS (Command Query Responsibility Segregation):** Write operations (Commands) are separated from read operations (Queries) to optimize performance and scalability.
- **Event Sourcing:** All changes to the application state are stored as a sequence of immutable events. This provides a full audit log and allows for rebuilding state.
- **Event Stream:** A central message broker (Kafka) acts as the event backbone for asynchronous communication between services.
- **Read Models:** Projections are created from the event stream to generate denormalized, optimized data stores for efficient querying.
- **Microservices:** The architecture is composed of decoupled services, each aligned with a specific business capability.
- **Authentication:** Security is handled via OAuth2 and JWT, managed by a dedicated identity provider.

## 9. Business Capabilities & Service Mapping

| Business Capability        | Corresponding Microservices                                              |
| -------------------------- | ------------------------------------------------------------------------ |
| **Identity & Access**      | User Service, Membership Service, Authorization Service                  |
| **Project Management**     | Organization Service, Project Service                                    |
| **Supply Chain**           | Vendor Service, Materials Service, Procurement Service, Delivery Service |
| **Financial Management**   | Financials Service                                                       |
| **Construction Execution** | Subcontractor Service, Review Service                                    |
| **Collaboration**          | Notification Service, Workflow Service, DMS Service                      |
| **Reporting**              | Reporting & Analytics Service                                            |

## 10. Marketplace & Bidding System

The integrated marketplace is a core component of ConstructHub, connecting constructors with material suppliers and service subcontractors through a competitive and transparent bidding system.

### a. Bidding for Material Suppliers

- **Credit-Based Participation:** To ensure the seriousness of proposals and prevent spam, suppliers must use 'Bid Credits' to participate in tenders. A base number of free credits are provided monthly, with additional credit packages available for purchase.
- **Tender Types:**
  - **Open Bidding:** A public request for materials where any qualified supplier can submit a bid.
  - **Invited Bidding:** A private request sent by the constructor to a pre-selected list of trusted suppliers.
- **Premium Bids:** Suppliers can use premium 'Golden Bids' to have their offers highlighted and featured at the top of the proposal list.
- **Smart Connection to Logistics:** The system is intelligently linked to the on-site logistics module. A bid is only considered 'successful' and ready for commission calculation after the site manager confirms the delivery of goods via the mobile app.
- **Fairness Mechanism:** If a constructor cancels a tender after bids have been placed, the 'Bid Credits' are automatically refunded to the participating suppliers.

### b. Bidding for Service Subcontractors

The same bidding mechanism is extended to service subcontractors (e.g., concrete pouring, painting, plumbing) with additional features tailored for service-based work.

- **Flexible Quoting Units:** The system supports various pricing units standard to the construction industry, including:
  - Per square meter (e.g., for painting, plastering).
  - Per cubic meter (e.g., for concrete).
  - Per unit/point (e.g., for electrical installations).
  - Lump-Sum (Kontrat) for the entire scope of work.
- **Reputation and Portfolio System:** Price is not the only factor. Subcontractors have a profile that includes a portfolio of past work (with images) and a rating score (1-5 stars) based on feedback from previous projects on quality, timeliness, and professionalism.
- **Integration with Work Progress Module:** Payments are linked to verified work progress. A subcontractor requests a payment for a completed phase (e.g., 'first-floor concrete pour completed'). The site engineer verifies the work in the app, and the system automatically generates the invoice based on the agreed-upon bid price, ensuring transparent and timely payments.

## 11. Security

- **Authentication:** OAuth2 & OpenID Connect (OIDC) for secure, token-based authentication.
- **Authorization:** Role-Based Access Control (RBAC) to enforce granular permissions. JWTs will carry user roles and permissions.
- **Data Isolation:** Strong data partitioning between Organizations (tenants) at the database and application level. Project isolation within an organization.
- **Audit Logs:** The Event Sourcing nature of the system provides a comprehensive and immutable audit log of all state-changing actions.

## 12. Technology Stack

- **Frontend:** React, TypeScript
- **Backend:** Java, Spring Boot
- **Messaging:** Apache Kafka
- **Database:** PostgreSQL
- **Storage:** S3-compatible object storage
- **Authentication:** Keycloak
- **Deployment:** Docker, Kubernetes

## 13. Roadmap

- **Phase 1 (MVP):** User Management, Organizations, Projects.
- **Phase 2:** Vendors, Procurement, Inventory.
- **Phase 3:** Deliveries, Financials, Workflow.
- **Phase 4:** Reporting, AI Document Analysis.

## 14. Business Model

- **Primary Customers:** Construction Companies, General Contractors.
- **Revenue Streams:**
  - **Marketplace Transaction Fees (Commission):** The commission structure is dynamic and designed to be fair, encouraging high-volume transactions.
    - **Tiered & Category-Specific Rates:** Commission rates are not flat. They are tiered and tailored to the margin of each category (e.g., a low, tonnage-based fee for steel vs. a higher percentage for decorative fixtures).
    - **Project-Based Commission Cap:** A maximum commission ceiling (Cap Fee) is defined per material category for each individual project. Once a supplier's total commission for a category (e.g., steel) in a project reaches this cap, all their subsequent sales in that same category for that project become commission-free. This is a key feature to ensure platform loyalty and prevent offline transactions.
  - **Sale of Bidding Credits:** Suppliers and subcontractors purchase packages of 'Bid Credits' to participate in a larger number of tenders.
  - **Premium Listings & 'Golden Bids':** Fees for featured placement in tender results.
  - **SaaS Subscriptions:** For constructors who need advanced features like unlimited OCR scanning or management of a high volume of projects.
- **Secondary Customers:** Material Vendors & Suppliers.
- **Pricing Tiers:**
  - **Starter:** Basic features for small teams.
  - **Professional:** Advanced features for mid-sized companies.
  - **Enterprise:** Full feature set with premium support and customizations.

## 15. Future Features

- Native Mobile App (iOS/Android)
- BIM (Building Information Modeling) Integration
- IoT Sensor Integration for equipment and material tracking
- AI-based Risk Prediction
- AI-based Cost Forecasting
- **AI-Powered Planning:** Expanding the material estimator to provide more granular cost, quantity, and timeline predictions throughout the project lifecycle.
