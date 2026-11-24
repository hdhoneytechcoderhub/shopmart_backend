-- V1: full baseline schema for a brand-new database.
--
-- This codebase originally relied on Hibernate ddl-auto=update, so the first Flyway
-- deployment used baseline-on-migrate (baseline-version 1) against an already-populated
-- schema and there was deliberately no V1 file. That made a truly empty database impossible
-- to build with Flyway alone: V2+ only contain incremental changes.
--
-- This file is the schema of the current JPA model (generated from Hibernate's own DDL and
-- then pg_dump'd), so Flyway can create a fresh database from zero. Existing databases that
-- are already baselined at version 1 ignore this migration; it is never run against them,
-- so no existing data or checksums are affected.
--
-- Keep this file frozen. All future schema changes belong in new, higher-versioned files.

CREATE TABLE public.addresses (
    is_default boolean,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    city character varying(255),
    country character varying(255),
    email character varying(255),
    full_name character varying(255),
    label character varying(255),
    line1 character varying(255),
    line2 character varying(255),
    phone character varying(255),
    postal_code character varying(255),
    state character varying(255)
);


--
-- Name: addresses_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.addresses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: addresses_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.addresses_id_seq OWNED BY public.addresses.id;


--
-- Name: attendance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attendance (
    total_hours double precision,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    login_time timestamp(6) with time zone NOT NULL,
    logout_time timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL
);


--
-- Name: attendance_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.attendance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: attendance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.attendance_id_seq OWNED BY public.attendance.id;


--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_logs (
    created_at timestamp(6) with time zone NOT NULL,
    entity_id bigint,
    id bigint NOT NULL,
    user_id bigint,
    action character varying(80) NOT NULL,
    entity_type character varying(80),
    detail character varying(2000)
);


--
-- Name: audit_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.audit_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: audit_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.audit_logs_id_seq OWNED BY public.audit_logs.id;


--
-- Name: banners; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.banners (
    active boolean DEFAULT true NOT NULL,
    "position" integer DEFAULT 0 NOT NULL,
    created_at timestamp(6) with time zone,
    ends_at timestamp(6) with time zone,
    id bigint NOT NULL,
    starts_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    image_url character varying(255) NOT NULL,
    link_url character varying(255),
    subtitle character varying(255),
    title character varying(255) NOT NULL
);


--
-- Name: banners_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.banners_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: banners_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.banners_id_seq OWNED BY public.banners.id;


--
-- Name: blog_post_tags; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.blog_post_tags (
    post_id bigint NOT NULL,
    tag character varying(255)
);


--
-- Name: blog_posts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.blog_posts (
    author_id bigint,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    published_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    excerpt character varying(500),
    content text,
    cover_image character varying(255),
    slug character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    CONSTRAINT blog_posts_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PUBLISHED'::character varying])::text[])))
);


--
-- Name: blog_posts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.blog_posts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: blog_posts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.blog_posts_id_seq OWNED BY public.blog_posts.id;


--
-- Name: brands; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.brands (
    active boolean NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    description character varying(1000),
    banner_url character varying(255),
    logo_url character varying(255),
    meta_description character varying(255),
    meta_title character varying(255),
    name character varying(255) NOT NULL,
    slug character varying(255) NOT NULL
);


--
-- Name: brands_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.brands_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: brands_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.brands_id_seq OWNED BY public.brands.id;


--
-- Name: cart_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cart_items (
    quantity integer NOT NULL,
    unit_price numeric(12,2) NOT NULL,
    cart_id bigint NOT NULL,
    id bigint NOT NULL,
    product_id bigint NOT NULL,
    variant_id bigint
);


--
-- Name: cart_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cart_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cart_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cart_items_id_seq OWNED BY public.cart_items.id;


--
-- Name: carts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.carts (
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL
);


--
-- Name: carts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.carts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: carts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.carts_id_seq OWNED BY public.carts.id;


--
-- Name: categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categories (
    active boolean NOT NULL,
    sort_order integer NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    parent_id bigint,
    updated_at timestamp(6) with time zone,
    description character varying(1000),
    banner_url character varying(255),
    meta_description character varying(255),
    meta_title character varying(255),
    name character varying(255) NOT NULL,
    slug character varying(255) NOT NULL
);


--
-- Name: categories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.categories_id_seq OWNED BY public.categories.id;


--
-- Name: compliance_documents; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compliance_documents (
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    uploaded_by bigint NOT NULL,
    status character varying(20) NOT NULL,
    reason character varying(1000),
    certificate_url character varying(255),
    document_storage_key character varying(255),
    document_url character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    CONSTRAINT compliance_documents_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: compliance_documents_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.compliance_documents_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: compliance_documents_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.compliance_documents_id_seq OWNED BY public.compliance_documents.id;


--
-- Name: contact_messages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.contact_messages (
    handled boolean NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    message character varying(4000) NOT NULL,
    email character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    phone character varying(255),
    subject character varying(255) NOT NULL
);


--
-- Name: contact_messages_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.contact_messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: contact_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.contact_messages_id_seq OWNED BY public.contact_messages.id;


--
-- Name: coupon_redemptions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.coupon_redemptions (
    discount_applied numeric(12,2) NOT NULL,
    coupon_id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    order_id bigint,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL
);


--
-- Name: coupon_redemptions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.coupon_redemptions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: coupon_redemptions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.coupon_redemptions_id_seq OWNED BY public.coupon_redemptions.id;


--
-- Name: coupons; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.coupons (
    active boolean NOT NULL,
    discount_value numeric(12,2) NOT NULL,
    max_discount_amount numeric(12,2),
    min_order_amount numeric(12,2),
    per_user_limit integer,
    usage_limit integer,
    used_count integer NOT NULL,
    created_at timestamp(6) with time zone,
    expires_at timestamp(6) with time zone,
    id bigint NOT NULL,
    starts_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    code character varying(255) NOT NULL,
    description character varying(255),
    discount_type character varying(255) NOT NULL,
    CONSTRAINT coupons_discount_type_check CHECK (((discount_type)::text = ANY ((ARRAY['PERCENTAGE'::character varying, 'FIXED'::character varying])::text[])))
);


--
-- Name: coupons_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.coupons_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: coupons_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.coupons_id_seq OWNED BY public.coupons.id;


--
-- Name: custom_role_permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.custom_role_permissions (
    custom_role_id bigint NOT NULL,
    permission_id bigint NOT NULL
);


--
-- Name: custom_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.custom_roles (
    active boolean NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    description character varying(255),
    name character varying(255) NOT NULL
);


--
-- Name: custom_roles_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.custom_roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: custom_roles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.custom_roles_id_seq OWNED BY public.custom_roles.id;


--
-- Name: customer_notes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customer_notes (
    author_id bigint NOT NULL,
    created_at timestamp(6) with time zone,
    customer_id bigint NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    note character varying(2000) NOT NULL
);


--
-- Name: customer_notes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.customer_notes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customer_notes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.customer_notes_id_seq OWNED BY public.customer_notes.id;


--
-- Name: device_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.device_tokens (
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    platform character varying(16),
    app_version character varying(32),
    token character varying(512) NOT NULL
);


--
-- Name: device_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.device_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: device_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.device_tokens_id_seq OWNED BY public.device_tokens.id;


--
-- Name: loyalty_accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loyalty_accounts (
    balance integer DEFAULT 0 NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL
);


--
-- Name: loyalty_accounts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.loyalty_accounts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: loyalty_accounts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.loyalty_accounts_id_seq OWNED BY public.loyalty_accounts.id;


--
-- Name: loyalty_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.loyalty_transactions (
    points integer NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    order_id bigint,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    description character varying(255),
    type character varying(255) NOT NULL,
    CONSTRAINT loyalty_transactions_type_check CHECK (((type)::text = ANY ((ARRAY['EARN'::character varying, 'REDEEM'::character varying, 'ADJUST'::character varying])::text[])))
);


--
-- Name: loyalty_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.loyalty_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: loyalty_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.loyalty_transactions_id_seq OWNED BY public.loyalty_transactions.id;


--
-- Name: machine_images; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.machine_images (
    machine_id bigint NOT NULL,
    image_url character varying(255)
);


--
-- Name: machines; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.machines (
    is_deleted boolean NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    description character varying(2000),
    brand character varying(255),
    model_number character varying(255),
    name character varying(255) NOT NULL
);


--
-- Name: machines_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.machines_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: machines_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.machines_id_seq OWNED BY public.machines.id;


--
-- Name: notification_campaigns; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification_campaigns (
    recipient_count integer,
    created_at timestamp(6) with time zone,
    created_by bigint,
    id bigint NOT NULL,
    scheduled_at timestamp(6) with time zone,
    sent_at timestamp(6) with time zone,
    template_id bigint,
    updated_at timestamp(6) with time zone,
    channel character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    target_type character varying(20) NOT NULL,
    failure_reason character varying(500),
    message character varying(4000) NOT NULL,
    link character varying(255),
    name character varying(255) NOT NULL,
    target_role character varying(255),
    target_segment character varying(255),
    title character varying(255) NOT NULL,
    CONSTRAINT notification_campaigns_channel_check CHECK (((channel)::text = ANY ((ARRAY['IN_APP'::character varying, 'EMAIL'::character varying, 'PUSH'::character varying, 'SMS'::character varying])::text[]))),
    CONSTRAINT notification_campaigns_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SCHEDULED'::character varying, 'SENDING'::character varying, 'SENT'::character varying, 'CANCELLED'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT notification_campaigns_target_type_check CHECK (((target_type)::text = ANY ((ARRAY['ALL'::character varying, 'ROLE'::character varying, 'SEGMENT'::character varying])::text[])))
);


--
-- Name: notification_campaigns_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notification_campaigns_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notification_campaigns_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notification_campaigns_id_seq OWNED BY public.notification_campaigns.id;


--
-- Name: notification_templates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notification_templates (
    active boolean NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    channel character varying(20) NOT NULL,
    code character varying(100) NOT NULL,
    body character varying(4000) NOT NULL,
    description character varying(255),
    subject character varying(255),
    CONSTRAINT notification_templates_channel_check CHECK (((channel)::text = ANY ((ARRAY['IN_APP'::character varying, 'EMAIL'::character varying, 'PUSH'::character varying, 'SMS'::character varying])::text[])))
);


--
-- Name: notification_templates_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notification_templates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notification_templates_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notification_templates_id_seq OWNED BY public.notification_templates.id;


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    is_read boolean NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    message character varying(1000),
    link character varying(255),
    title character varying(255) NOT NULL,
    type character varying(255) NOT NULL,
    CONSTRAINT notifications_type_check CHECK (((type)::text = ANY ((ARRAY['ORDER'::character varying, 'PAYMENT'::character varying, 'PROMO'::character varying, 'REVIEW'::character varying, 'SYSTEM'::character varying])::text[])))
);


--
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notifications_id_seq OWNED BY public.notifications.id;


--
-- Name: order_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_items (
    line_total numeric(12,2) NOT NULL,
    quantity integer NOT NULL,
    unit_price numeric(12,2) NOT NULL,
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    product_id bigint NOT NULL,
    vendor_id bigint,
    product_name character varying(255) NOT NULL,
    thumbnail character varying(255)
);


--
-- Name: order_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.order_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: order_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.order_items_id_seq OWNED BY public.order_items.id;


--
-- Name: order_status_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.order_status_history (
    created_at timestamp(6) with time zone NOT NULL,
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    status character varying(20) NOT NULL,
    note character varying(500),
    CONSTRAINT order_status_history_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'PROCESSING'::character varying, 'PACKED'::character varying, 'SHIPPED'::character varying, 'DELIVERED'::character varying, 'CANCELLED'::character varying, 'RETURNED'::character varying])::text[])))
);


--
-- Name: order_status_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.order_status_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: order_status_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.order_status_history_id_seq OWNED BY public.order_status_history.id;


--
-- Name: orders; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orders (
    discount numeric(12,2) NOT NULL,
    shipping_fee numeric(12,2) NOT NULL,
    subtotal numeric(12,2) NOT NULL,
    total numeric(12,2) NOT NULL,
    admin_email_sent_at timestamp(6) with time zone,
    confirmation_email_sent_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    paid_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    client_request_id character varying(100),
    courier_partner character varying(255),
    customer_email character varying(255),
    order_number character varying(255) NOT NULL,
    payment_method character varying(255),
    payment_status character varying(255) NOT NULL,
    razorpay_order_id character varying(255),
    razorpay_payment_id character varying(255),
    ship_city character varying(255),
    ship_country character varying(255),
    ship_email character varying(255),
    ship_line1 character varying(255),
    ship_line2 character varying(255),
    ship_name character varying(255),
    ship_phone character varying(255),
    ship_postal_code character varying(255),
    ship_state character varying(255),
    status character varying(255) NOT NULL,
    tracking_number character varying(255),
    CONSTRAINT orders_payment_status_check CHECK (((payment_status)::text = ANY ((ARRAY['PENDING'::character varying, 'PAID'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying, 'REFUNDED'::character varying, 'PARTIALLY_REFUNDED'::character varying])::text[]))),
    CONSTRAINT orders_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'CONFIRMED'::character varying, 'PROCESSING'::character varying, 'PACKED'::character varying, 'SHIPPED'::character varying, 'DELIVERED'::character varying, 'CANCELLED'::character varying, 'RETURNED'::character varying])::text[])))
);


--
-- Name: orders_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.orders_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: orders_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.orders_id_seq OWNED BY public.orders.id;


--
-- Name: otp_verifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.otp_verifications (
    used boolean NOT NULL,
    created_at timestamp(6) with time zone,
    expires_at timestamp(6) with time zone NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    code character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    purpose character varying(255) NOT NULL,
    CONSTRAINT otp_verifications_purpose_check CHECK (((purpose)::text = ANY ((ARRAY['EMAIL_VERIFICATION'::character varying, 'PASSWORD_RESET'::character varying])::text[])))
);


--
-- Name: otp_verifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.otp_verifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: otp_verifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.otp_verifications_id_seq OWNED BY public.otp_verifications.id;


--
-- Name: payment_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payment_transactions (
    amount numeric(38,2),
    created_at timestamp(6) with time zone NOT NULL,
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    payment_id bigint,
    status character varying(20) NOT NULL,
    idempotency_key character varying(100),
    note character varying(500),
    gateway_order_id character varying(255),
    provider character varying(255),
    transaction_id character varying(255)
);


--
-- Name: payment_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payment_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payment_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payment_transactions_id_seq OWNED BY public.payment_transactions.id;


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    amount numeric(12,2) NOT NULL,
    refunded_amount numeric(12,2),
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    failure_reason character varying(255),
    gateway_ref character varying(255),
    method character varying(255) NOT NULL,
    provider character varying(255),
    status character varying(255) NOT NULL,
    transaction_id character varying(255),
    CONSTRAINT payments_status_check CHECK (((status)::text = ANY ((ARRAY['CREATED'::character varying, 'PENDING'::character varying, 'SUCCESS'::character varying, 'FAILED'::character varying, 'CANCELLED'::character varying, 'REFUNDED'::character varying, 'PARTIALLY_REFUNDED'::character varying])::text[])))
);


--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: payments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.payments_id_seq OWNED BY public.payments.id;


--
-- Name: permission_groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permission_groups (
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    description character varying(255),
    name character varying(255) NOT NULL
);


--
-- Name: permission_groups_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.permission_groups_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: permission_groups_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.permission_groups_id_seq OWNED BY public.permission_groups.id;


--
-- Name: permissions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.permissions (
    created_at timestamp(6) with time zone,
    group_id bigint,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    code character varying(100) NOT NULL,
    description character varying(255),
    module character varying(255),
    name character varying(255) NOT NULL,
    screen_key character varying(255)
);


--
-- Name: permissions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.permissions_id_seq OWNED BY public.permissions.id;


--
-- Name: product_images; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_images (
    "position" integer NOT NULL,
    id bigint NOT NULL,
    product_id bigint NOT NULL,
    alt character varying(255),
    storage_key character varying(255),
    url character varying(255) NOT NULL
);


--
-- Name: product_images_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_images_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_images_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_images_id_seq OWNED BY public.product_images.id;


--
-- Name: product_variants; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_variants (
    price numeric(12,2),
    stock integer NOT NULL,
    id bigint NOT NULL,
    product_id bigint NOT NULL,
    color character varying(255),
    size character varying(255),
    sku character varying(255)
);


--
-- Name: product_variants_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_variants_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_variants_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_variants_id_seq OWNED BY public.product_variants.id;


--
-- Name: product_views; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.product_views (
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    product_id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint
);


--
-- Name: product_views_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.product_views_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: product_views_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.product_views_id_seq OWNED BY public.product_views.id;


--
-- Name: products; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.products (
    featured boolean NOT NULL,
    low_stock_threshold integer,
    price numeric(12,2) NOT NULL,
    rating_average numeric(3,2),
    rating_count integer NOT NULL,
    sale_price numeric(12,2),
    stock integer NOT NULL,
    brand_id bigint,
    category_id bigint,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    vendor_id bigint,
    meta_keywords character varying(500),
    rejection_reason character varying(1000),
    description character varying(5000),
    meta_description character varying(255),
    meta_title character varying(255),
    name character varying(255) NOT NULL,
    sku character varying(255),
    slug character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    CONSTRAINT products_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'PENDING_APPROVAL'::character varying, 'ACTIVE'::character varying, 'REJECTED'::character varying, 'ARCHIVED'::character varying, 'OUT_OF_STOCK'::character varying])::text[])))
);


--
-- Name: products_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.products_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: products_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.products_id_seq OWNED BY public.products.id;


--
-- Name: push_delivery_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.push_delivery_logs (
    attempted_tokens integer,
    delivered_tokens integer,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    status character varying(20) NOT NULL,
    failure_reason character varying(500),
    title character varying(255),
    CONSTRAINT push_delivery_logs_status_check CHECK (((status)::text = ANY ((ARRAY['SENT'::character varying, 'PARTIAL'::character varying, 'FAILED'::character varying, 'NO_DEVICES'::character varying])::text[])))
);


--
-- Name: push_delivery_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.push_delivery_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: push_delivery_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.push_delivery_logs_id_seq OWNED BY public.push_delivery_logs.id;


--
-- Name: push_device_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.push_device_tokens (
    active boolean NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    last_seen_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    platform character varying(10) NOT NULL,
    token character varying(500) NOT NULL,
    CONSTRAINT push_device_tokens_platform_check CHECK (((platform)::text = ANY ((ARRAY['ANDROID'::character varying, 'IOS'::character varying, 'WEB'::character varying])::text[])))
);


--
-- Name: push_device_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.push_device_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: push_device_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.push_device_tokens_id_seq OWNED BY public.push_device_tokens.id;


--
-- Name: referral_codes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.referral_codes (
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    code character varying(20) NOT NULL
);


--
-- Name: referral_codes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.referral_codes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: referral_codes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.referral_codes_id_seq OWNED BY public.referral_codes.id;


--
-- Name: referrals; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.referrals (
    reward_amount numeric(38,2),
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    referred_user_id bigint NOT NULL,
    referrer_id bigint NOT NULL,
    updated_at timestamp(6) with time zone
);


--
-- Name: referrals_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.referrals_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: referrals_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.referrals_id_seq OWNED BY public.referrals.id;


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    revoked boolean NOT NULL,
    created_at timestamp(6) with time zone,
    expires_at timestamp(6) with time zone NOT NULL,
    id bigint NOT NULL,
    last_used_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    ip_address character varying(64),
    token character varying(512) NOT NULL,
    device_info character varying(255)
);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refresh_tokens_id_seq OWNED BY public.refresh_tokens.id;


--
-- Name: report_schedules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.report_schedules (
    enabled boolean NOT NULL,
    created_at timestamp(6) with time zone,
    created_by bigint,
    id bigint NOT NULL,
    last_run_at timestamp(6) with time zone,
    next_run_at timestamp(6) with time zone NOT NULL,
    updated_at timestamp(6) with time zone,
    frequency character varying(20) NOT NULL,
    report_type character varying(20) NOT NULL,
    recipient_emails character varying(1000) NOT NULL,
    name character varying(255) NOT NULL,
    CONSTRAINT report_schedules_frequency_check CHECK (((frequency)::text = ANY ((ARRAY['DAILY'::character varying, 'WEEKLY'::character varying, 'MONTHLY'::character varying])::text[]))),
    CONSTRAINT report_schedules_report_type_check CHECK (((report_type)::text = ANY ((ARRAY['GROWTH_TREND'::character varying, 'KPI_SUMMARY'::character varying])::text[])))
);


--
-- Name: report_schedules_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.report_schedules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: report_schedules_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.report_schedules_id_seq OWNED BY public.report_schedules.id;


--
-- Name: return_requests; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.return_requests (
    refund_amount numeric(12,2),
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    order_id bigint NOT NULL,
    processed_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    admin_note character varying(1000),
    reason character varying(1000) NOT NULL,
    gateway_refund_id character varying(255),
    status character varying(255) NOT NULL,
    CONSTRAINT return_requests_status_check CHECK (((status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'REFUNDED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: return_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.return_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: return_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.return_requests_id_seq OWNED BY public.return_requests.id;


--
-- Name: reviews; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reviews (
    rating integer NOT NULL,
    verified_purchase boolean NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    product_id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    comment character varying(2000),
    status character varying(255) NOT NULL,
    title character varying(255),
    CONSTRAINT reviews_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: reviews_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.reviews_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: reviews_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.reviews_id_seq OWNED BY public.reviews.id;


--
-- Name: search_logs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.search_logs (
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    result_count bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint,
    query_text character varying(255) NOT NULL
);


--
-- Name: search_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.search_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: search_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.search_logs_id_seq OWNED BY public.search_logs.id;


--
-- Name: service_bookings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.service_bookings (
    rating integer,
    created_at timestamp(6) with time zone,
    engineer_id bigint,
    id bigint NOT NULL,
    scheduled_at timestamp(6) with time zone,
    service_item_id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    status character varying(20) NOT NULL,
    rating_comment character varying(1000),
    completion_notes character varying(2000),
    notes character varying(2000),
    address character varying(255) NOT NULL,
    phone character varying(255),
    CONSTRAINT service_bookings_status_check CHECK (((status)::text = ANY ((ARRAY['REQUESTED'::character varying, 'ASSIGNED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: service_bookings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.service_bookings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: service_bookings_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.service_bookings_id_seq OWNED BY public.service_bookings.id;


--
-- Name: service_categories; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.service_categories (
    active boolean NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    description character varying(1000),
    image_url character varying(255),
    name character varying(255) NOT NULL
);


--
-- Name: service_categories_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.service_categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: service_categories_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.service_categories_id_seq OWNED BY public.service_categories.id;


--
-- Name: service_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.service_items (
    active boolean NOT NULL,
    duration_minutes integer,
    price numeric(38,2) NOT NULL,
    category_id bigint,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    description character varying(2000),
    image_url character varying(255),
    name character varying(255) NOT NULL
);


--
-- Name: service_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.service_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: service_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.service_items_id_seq OWNED BY public.service_items.id;


--
-- Name: settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.settings (
    is_public boolean NOT NULL,
    created_at timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    setting_key character varying(120) NOT NULL,
    setting_value character varying(4000)
);


--
-- Name: stock_transfers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.stock_transfers (
    quantity integer NOT NULL,
    created_at timestamp(6) with time zone,
    from_warehouse_id bigint NOT NULL,
    id bigint NOT NULL,
    product_id bigint NOT NULL,
    to_warehouse_id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    note character varying(255),
    status character varying(255) NOT NULL,
    CONSTRAINT stock_transfers_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: stock_transfers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.stock_transfers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: stock_transfers_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.stock_transfers_id_seq OWNED BY public.stock_transfers.id;


--
-- Name: translations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.translations (
    created_at timestamp(6) with time zone,
    entity_id bigint NOT NULL,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    locale character varying(16) NOT NULL,
    entity_type character varying(64) NOT NULL,
    field character varying(64) NOT NULL,
    value character varying(4000) NOT NULL
);


--
-- Name: translations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.translations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: translations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.translations_id_seq OWNED BY public.translations.id;


--
-- Name: user_custom_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_custom_roles (
    custom_role_id bigint NOT NULL,
    user_id bigint NOT NULL
);


--
-- Name: user_roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_roles (
    user_id bigint NOT NULL,
    role character varying(255),
    CONSTRAINT user_roles_role_check CHECK (((role)::text = ANY ((ARRAY['ROLE_CUSTOMER'::character varying, 'ROLE_ADMIN'::character varying, 'ROLE_VENDOR'::character varying, 'ROLE_SUPER_ADMIN'::character varying, 'ROLE_ENGINEER'::character varying])::text[])))
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    email_verified boolean NOT NULL,
    enabled boolean NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    avatar_url character varying(255),
    email character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    phone character varying(255),
    provider character varying(255) DEFAULT 'LOCAL'::character varying NOT NULL,
    provider_id character varying(255),
    CONSTRAINT users_provider_check CHECK (((provider)::text = ANY ((ARRAY['LOCAL'::character varying, 'GOOGLE'::character varying])::text[])))
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: vendor_payouts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vendor_payouts (
    amount numeric(12,2) NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    paid_at timestamp(6) with time zone,
    period_end timestamp(6) with time zone,
    period_start timestamp(6) with time zone,
    updated_at timestamp(6) with time zone,
    vendor_id bigint NOT NULL,
    note character varying(255),
    status character varying(255) NOT NULL,
    CONSTRAINT vendor_payouts_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'PAID'::character varying])::text[])))
);


--
-- Name: vendor_payouts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vendor_payouts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vendor_payouts_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vendor_payouts_id_seq OWNED BY public.vendor_payouts.id;


--
-- Name: vendors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vendors (
    commission_rate numeric(5,2) NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL,
    description character varying(2000),
    contact_email character varying(255),
    contact_phone character varying(255),
    logo_url character varying(255),
    slug character varying(255) NOT NULL,
    status character varying(255) NOT NULL,
    store_name character varying(255) NOT NULL,
    CONSTRAINT vendors_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'APPROVED'::character varying, 'SUSPENDED'::character varying, 'REJECTED'::character varying])::text[])))
);


--
-- Name: vendors_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vendors_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vendors_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vendors_id_seq OWNED BY public.vendors.id;


--
-- Name: wallet_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wallet_transactions (
    amount numeric(38,2) NOT NULL,
    balance_after numeric(38,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    id bigint NOT NULL,
    wallet_id bigint NOT NULL,
    type character varying(10) NOT NULL,
    reason character varying(255)
);


--
-- Name: wallet_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.wallet_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: wallet_transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.wallet_transactions_id_seq OWNED BY public.wallet_transactions.id;


--
-- Name: wallets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wallets (
    balance numeric(38,2) NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL
);


--
-- Name: wallets_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.wallets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: wallets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.wallets_id_seq OWNED BY public.wallets.id;


--
-- Name: warehouse_inventory; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.warehouse_inventory (
    quantity integer NOT NULL,
    reserved integer NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    product_id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    warehouse_id bigint NOT NULL
);


--
-- Name: warehouse_inventory_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.warehouse_inventory_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: warehouse_inventory_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.warehouse_inventory_id_seq OWNED BY public.warehouse_inventory.id;


--
-- Name: warehouses; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.warehouses (
    active boolean NOT NULL,
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    address_line character varying(255),
    city character varying(255),
    code character varying(255) NOT NULL,
    country character varying(255),
    name character varying(255) NOT NULL,
    postal_code character varying(255),
    state character varying(255)
);


--
-- Name: warehouses_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.warehouses_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: warehouses_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.warehouses_id_seq OWNED BY public.warehouses.id;


--
-- Name: webhook_events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.webhook_events (
    id bigint NOT NULL,
    received_at timestamp(6) with time zone NOT NULL,
    provider character varying(30) NOT NULL,
    event_id character varying(128) NOT NULL
);


--
-- Name: webhook_events_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.webhook_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: webhook_events_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.webhook_events_id_seq OWNED BY public.webhook_events.id;


--
-- Name: wishlist_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.wishlist_items (
    created_at timestamp(6) with time zone,
    id bigint NOT NULL,
    product_id bigint NOT NULL,
    updated_at timestamp(6) with time zone,
    user_id bigint NOT NULL
);


--
-- Name: wishlist_items_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.wishlist_items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: wishlist_items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.wishlist_items_id_seq OWNED BY public.wishlist_items.id;


--
-- Name: addresses id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.addresses ALTER COLUMN id SET DEFAULT nextval('public.addresses_id_seq'::regclass);


--
-- Name: attendance id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance ALTER COLUMN id SET DEFAULT nextval('public.attendance_id_seq'::regclass);


--
-- Name: audit_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs ALTER COLUMN id SET DEFAULT nextval('public.audit_logs_id_seq'::regclass);


--
-- Name: banners id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.banners ALTER COLUMN id SET DEFAULT nextval('public.banners_id_seq'::regclass);


--
-- Name: blog_posts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blog_posts ALTER COLUMN id SET DEFAULT nextval('public.blog_posts_id_seq'::regclass);


--
-- Name: brands id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brands ALTER COLUMN id SET DEFAULT nextval('public.brands_id_seq'::regclass);


--
-- Name: cart_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items ALTER COLUMN id SET DEFAULT nextval('public.cart_items_id_seq'::regclass);


--
-- Name: carts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts ALTER COLUMN id SET DEFAULT nextval('public.carts_id_seq'::regclass);


--
-- Name: categories id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories ALTER COLUMN id SET DEFAULT nextval('public.categories_id_seq'::regclass);


--
-- Name: compliance_documents id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliance_documents ALTER COLUMN id SET DEFAULT nextval('public.compliance_documents_id_seq'::regclass);


--
-- Name: contact_messages id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contact_messages ALTER COLUMN id SET DEFAULT nextval('public.contact_messages_id_seq'::regclass);


--
-- Name: coupon_redemptions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coupon_redemptions ALTER COLUMN id SET DEFAULT nextval('public.coupon_redemptions_id_seq'::regclass);


--
-- Name: coupons id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coupons ALTER COLUMN id SET DEFAULT nextval('public.coupons_id_seq'::regclass);


--
-- Name: custom_roles id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_roles ALTER COLUMN id SET DEFAULT nextval('public.custom_roles_id_seq'::regclass);


--
-- Name: customer_notes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_notes ALTER COLUMN id SET DEFAULT nextval('public.customer_notes_id_seq'::regclass);


--
-- Name: device_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_tokens ALTER COLUMN id SET DEFAULT nextval('public.device_tokens_id_seq'::regclass);


--
-- Name: loyalty_accounts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_accounts ALTER COLUMN id SET DEFAULT nextval('public.loyalty_accounts_id_seq'::regclass);


--
-- Name: loyalty_transactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_transactions ALTER COLUMN id SET DEFAULT nextval('public.loyalty_transactions_id_seq'::regclass);


--
-- Name: machines id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.machines ALTER COLUMN id SET DEFAULT nextval('public.machines_id_seq'::regclass);


--
-- Name: notification_campaigns id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_campaigns ALTER COLUMN id SET DEFAULT nextval('public.notification_campaigns_id_seq'::regclass);


--
-- Name: notification_templates id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_templates ALTER COLUMN id SET DEFAULT nextval('public.notification_templates_id_seq'::regclass);


--
-- Name: notifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications ALTER COLUMN id SET DEFAULT nextval('public.notifications_id_seq'::regclass);


--
-- Name: order_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items ALTER COLUMN id SET DEFAULT nextval('public.order_items_id_seq'::regclass);


--
-- Name: order_status_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_status_history ALTER COLUMN id SET DEFAULT nextval('public.order_status_history_id_seq'::regclass);


--
-- Name: orders id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders ALTER COLUMN id SET DEFAULT nextval('public.orders_id_seq'::regclass);


--
-- Name: otp_verifications id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otp_verifications ALTER COLUMN id SET DEFAULT nextval('public.otp_verifications_id_seq'::regclass);


--
-- Name: payment_transactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_transactions ALTER COLUMN id SET DEFAULT nextval('public.payment_transactions_id_seq'::regclass);


--
-- Name: payments id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments ALTER COLUMN id SET DEFAULT nextval('public.payments_id_seq'::regclass);


--
-- Name: permission_groups id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permission_groups ALTER COLUMN id SET DEFAULT nextval('public.permission_groups_id_seq'::regclass);


--
-- Name: permissions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions ALTER COLUMN id SET DEFAULT nextval('public.permissions_id_seq'::regclass);


--
-- Name: product_images id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_images ALTER COLUMN id SET DEFAULT nextval('public.product_images_id_seq'::regclass);


--
-- Name: product_variants id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_variants ALTER COLUMN id SET DEFAULT nextval('public.product_variants_id_seq'::regclass);


--
-- Name: product_views id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_views ALTER COLUMN id SET DEFAULT nextval('public.product_views_id_seq'::regclass);


--
-- Name: products id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products ALTER COLUMN id SET DEFAULT nextval('public.products_id_seq'::regclass);


--
-- Name: push_delivery_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.push_delivery_logs ALTER COLUMN id SET DEFAULT nextval('public.push_delivery_logs_id_seq'::regclass);


--
-- Name: push_device_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.push_device_tokens ALTER COLUMN id SET DEFAULT nextval('public.push_device_tokens_id_seq'::regclass);


--
-- Name: referral_codes id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.referral_codes ALTER COLUMN id SET DEFAULT nextval('public.referral_codes_id_seq'::regclass);


--
-- Name: referrals id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.referrals ALTER COLUMN id SET DEFAULT nextval('public.referrals_id_seq'::regclass);


--
-- Name: refresh_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.refresh_tokens_id_seq'::regclass);


--
-- Name: report_schedules id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.report_schedules ALTER COLUMN id SET DEFAULT nextval('public.report_schedules_id_seq'::regclass);


--
-- Name: return_requests id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.return_requests ALTER COLUMN id SET DEFAULT nextval('public.return_requests_id_seq'::regclass);


--
-- Name: reviews id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reviews ALTER COLUMN id SET DEFAULT nextval('public.reviews_id_seq'::regclass);


--
-- Name: search_logs id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.search_logs ALTER COLUMN id SET DEFAULT nextval('public.search_logs_id_seq'::regclass);


--
-- Name: service_bookings id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_bookings ALTER COLUMN id SET DEFAULT nextval('public.service_bookings_id_seq'::regclass);


--
-- Name: service_categories id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_categories ALTER COLUMN id SET DEFAULT nextval('public.service_categories_id_seq'::regclass);


--
-- Name: service_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_items ALTER COLUMN id SET DEFAULT nextval('public.service_items_id_seq'::regclass);


--
-- Name: stock_transfers id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfers ALTER COLUMN id SET DEFAULT nextval('public.stock_transfers_id_seq'::regclass);


--
-- Name: translations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.translations ALTER COLUMN id SET DEFAULT nextval('public.translations_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: vendor_payouts id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vendor_payouts ALTER COLUMN id SET DEFAULT nextval('public.vendor_payouts_id_seq'::regclass);


--
-- Name: vendors id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vendors ALTER COLUMN id SET DEFAULT nextval('public.vendors_id_seq'::regclass);


--
-- Name: wallet_transactions id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wallet_transactions ALTER COLUMN id SET DEFAULT nextval('public.wallet_transactions_id_seq'::regclass);


--
-- Name: wallets id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wallets ALTER COLUMN id SET DEFAULT nextval('public.wallets_id_seq'::regclass);


--
-- Name: warehouse_inventory id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouse_inventory ALTER COLUMN id SET DEFAULT nextval('public.warehouse_inventory_id_seq'::regclass);


--
-- Name: warehouses id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouses ALTER COLUMN id SET DEFAULT nextval('public.warehouses_id_seq'::regclass);


--
-- Name: webhook_events id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.webhook_events ALTER COLUMN id SET DEFAULT nextval('public.webhook_events_id_seq'::regclass);


--
-- Name: wishlist_items id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wishlist_items ALTER COLUMN id SET DEFAULT nextval('public.wishlist_items_id_seq'::regclass);


--
-- Name: addresses addresses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.addresses
    ADD CONSTRAINT addresses_pkey PRIMARY KEY (id);


--
-- Name: attendance attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_pkey PRIMARY KEY (id);


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: banners banners_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.banners
    ADD CONSTRAINT banners_pkey PRIMARY KEY (id);


--
-- Name: blog_posts blog_posts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blog_posts
    ADD CONSTRAINT blog_posts_pkey PRIMARY KEY (id);


--
-- Name: blog_posts blog_posts_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blog_posts
    ADD CONSTRAINT blog_posts_slug_key UNIQUE (slug);


--
-- Name: brands brands_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brands
    ADD CONSTRAINT brands_pkey PRIMARY KEY (id);


--
-- Name: brands brands_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.brands
    ADD CONSTRAINT brands_slug_key UNIQUE (slug);


--
-- Name: cart_items cart_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT cart_items_pkey PRIMARY KEY (id);


--
-- Name: carts carts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_pkey PRIMARY KEY (id);


--
-- Name: carts carts_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.carts
    ADD CONSTRAINT carts_user_id_key UNIQUE (user_id);


--
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);


--
-- Name: categories categories_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_slug_key UNIQUE (slug);


--
-- Name: compliance_documents compliance_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compliance_documents
    ADD CONSTRAINT compliance_documents_pkey PRIMARY KEY (id);


--
-- Name: contact_messages contact_messages_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.contact_messages
    ADD CONSTRAINT contact_messages_pkey PRIMARY KEY (id);


--
-- Name: coupon_redemptions coupon_redemptions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coupon_redemptions
    ADD CONSTRAINT coupon_redemptions_pkey PRIMARY KEY (id);


--
-- Name: coupons coupons_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coupons
    ADD CONSTRAINT coupons_code_key UNIQUE (code);


--
-- Name: coupons coupons_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.coupons
    ADD CONSTRAINT coupons_pkey PRIMARY KEY (id);


--
-- Name: custom_role_permissions custom_role_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_role_permissions
    ADD CONSTRAINT custom_role_permissions_pkey PRIMARY KEY (custom_role_id, permission_id);


--
-- Name: custom_roles custom_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_roles
    ADD CONSTRAINT custom_roles_pkey PRIMARY KEY (id);


--
-- Name: customer_notes customer_notes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customer_notes
    ADD CONSTRAINT customer_notes_pkey PRIMARY KEY (id);


--
-- Name: device_tokens device_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_tokens
    ADD CONSTRAINT device_tokens_pkey PRIMARY KEY (id);


--
-- Name: loyalty_accounts loyalty_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_accounts
    ADD CONSTRAINT loyalty_accounts_pkey PRIMARY KEY (id);


--
-- Name: loyalty_transactions loyalty_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_transactions
    ADD CONSTRAINT loyalty_transactions_pkey PRIMARY KEY (id);


--
-- Name: machines machines_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.machines
    ADD CONSTRAINT machines_pkey PRIMARY KEY (id);


--
-- Name: notification_campaigns notification_campaigns_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_campaigns
    ADD CONSTRAINT notification_campaigns_pkey PRIMARY KEY (id);


--
-- Name: notification_templates notification_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT notification_templates_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: order_items order_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT order_items_pkey PRIMARY KEY (id);


--
-- Name: order_status_history order_status_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_status_history
    ADD CONSTRAINT order_status_history_pkey PRIMARY KEY (id);


--
-- Name: orders orders_order_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_order_number_key UNIQUE (order_number);


--
-- Name: orders orders_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orders
    ADD CONSTRAINT orders_pkey PRIMARY KEY (id);


--
-- Name: otp_verifications otp_verifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otp_verifications
    ADD CONSTRAINT otp_verifications_pkey PRIMARY KEY (id);


--
-- Name: payment_transactions payment_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payment_transactions
    ADD CONSTRAINT payment_transactions_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: permission_groups permission_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permission_groups
    ADD CONSTRAINT permission_groups_pkey PRIMARY KEY (id);


--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);


--
-- Name: product_images product_images_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_images
    ADD CONSTRAINT product_images_pkey PRIMARY KEY (id);


--
-- Name: product_variants product_variants_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_variants
    ADD CONSTRAINT product_variants_pkey PRIMARY KEY (id);


--
-- Name: product_views product_views_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_views
    ADD CONSTRAINT product_views_pkey PRIMARY KEY (id);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: products products_slug_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_slug_key UNIQUE (slug);


--
-- Name: push_delivery_logs push_delivery_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.push_delivery_logs
    ADD CONSTRAINT push_delivery_logs_pkey PRIMARY KEY (id);


--
-- Name: push_device_tokens push_device_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.push_device_tokens
    ADD CONSTRAINT push_device_tokens_pkey PRIMARY KEY (id);


--
-- Name: referral_codes referral_codes_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.referral_codes
    ADD CONSTRAINT referral_codes_code_key UNIQUE (code);


--
-- Name: referral_codes referral_codes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.referral_codes
    ADD CONSTRAINT referral_codes_pkey PRIMARY KEY (id);


--
-- Name: referral_codes referral_codes_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.referral_codes
    ADD CONSTRAINT referral_codes_user_id_key UNIQUE (user_id);


--
-- Name: referrals referrals_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.referrals
    ADD CONSTRAINT referrals_pkey PRIMARY KEY (id);


--
-- Name: referrals referrals_referred_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.referrals
    ADD CONSTRAINT referrals_referred_user_id_key UNIQUE (referred_user_id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: report_schedules report_schedules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.report_schedules
    ADD CONSTRAINT report_schedules_pkey PRIMARY KEY (id);


--
-- Name: return_requests return_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.return_requests
    ADD CONSTRAINT return_requests_pkey PRIMARY KEY (id);


--
-- Name: reviews reviews_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);


--
-- Name: search_logs search_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.search_logs
    ADD CONSTRAINT search_logs_pkey PRIMARY KEY (id);


--
-- Name: service_bookings service_bookings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_bookings
    ADD CONSTRAINT service_bookings_pkey PRIMARY KEY (id);


--
-- Name: service_categories service_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_categories
    ADD CONSTRAINT service_categories_pkey PRIMARY KEY (id);


--
-- Name: service_items service_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_items
    ADD CONSTRAINT service_items_pkey PRIMARY KEY (id);


--
-- Name: settings settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.settings
    ADD CONSTRAINT settings_pkey PRIMARY KEY (setting_key);


--
-- Name: stock_transfers stock_transfers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.stock_transfers
    ADD CONSTRAINT stock_transfers_pkey PRIMARY KEY (id);


--
-- Name: translations translations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.translations
    ADD CONSTRAINT translations_pkey PRIMARY KEY (id);


--
-- Name: custom_roles uk_custom_role_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_roles
    ADD CONSTRAINT uk_custom_role_name UNIQUE (name);


--
-- Name: device_tokens uk_device_token; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.device_tokens
    ADD CONSTRAINT uk_device_token UNIQUE (token);


--
-- Name: loyalty_accounts uk_loyalty_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.loyalty_accounts
    ADD CONSTRAINT uk_loyalty_user UNIQUE (user_id);


--
-- Name: notification_templates uk_notification_template_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notification_templates
    ADD CONSTRAINT uk_notification_template_code UNIQUE (code);


--
-- Name: permissions uk_permission_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT uk_permission_code UNIQUE (code);


--
-- Name: permission_groups uk_permission_group_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permission_groups
    ADD CONSTRAINT uk_permission_group_name UNIQUE (name);


--
-- Name: push_device_tokens uk_push_device_tokens_token; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.push_device_tokens
    ADD CONSTRAINT uk_push_device_tokens_token UNIQUE (token);


--
-- Name: reviews uk_review_product_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reviews
    ADD CONSTRAINT uk_review_product_user UNIQUE (product_id, user_id);


--
-- Name: translations uk_translation; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.translations
    ADD CONSTRAINT uk_translation UNIQUE (entity_type, entity_id, locale, field);


--
-- Name: users uk_users_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_users_email UNIQUE (email);


--
-- Name: vendors uk_vendors_slug; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vendors
    ADD CONSTRAINT uk_vendors_slug UNIQUE (slug);


--
-- Name: vendors uk_vendors_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vendors
    ADD CONSTRAINT uk_vendors_user UNIQUE (user_id);


--
-- Name: webhook_events uk_webhook_provider_event; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.webhook_events
    ADD CONSTRAINT uk_webhook_provider_event UNIQUE (provider, event_id);


--
-- Name: warehouse_inventory uk_wh_inventory; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouse_inventory
    ADD CONSTRAINT uk_wh_inventory UNIQUE (warehouse_id, product_id);


--
-- Name: user_custom_roles user_custom_roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_custom_roles
    ADD CONSTRAINT user_custom_roles_pkey PRIMARY KEY (custom_role_id, user_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: vendor_payouts vendor_payouts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vendor_payouts
    ADD CONSTRAINT vendor_payouts_pkey PRIMARY KEY (id);


--
-- Name: vendors vendors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vendors
    ADD CONSTRAINT vendors_pkey PRIMARY KEY (id);


--
-- Name: wallet_transactions wallet_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wallet_transactions
    ADD CONSTRAINT wallet_transactions_pkey PRIMARY KEY (id);


--
-- Name: wallets wallets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wallets
    ADD CONSTRAINT wallets_pkey PRIMARY KEY (id);


--
-- Name: wallets wallets_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wallets
    ADD CONSTRAINT wallets_user_id_key UNIQUE (user_id);


--
-- Name: warehouse_inventory warehouse_inventory_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouse_inventory
    ADD CONSTRAINT warehouse_inventory_pkey PRIMARY KEY (id);


--
-- Name: warehouses warehouses_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouses
    ADD CONSTRAINT warehouses_code_key UNIQUE (code);


--
-- Name: warehouses warehouses_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.warehouses
    ADD CONSTRAINT warehouses_pkey PRIMARY KEY (id);


--
-- Name: webhook_events webhook_events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.webhook_events
    ADD CONSTRAINT webhook_events_pkey PRIMARY KEY (id);


--
-- Name: wishlist_items wishlist_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_pkey PRIMARY KEY (id);


--
-- Name: wishlist_items wishlist_items_user_id_product_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.wishlist_items
    ADD CONSTRAINT wishlist_items_user_id_product_id_key UNIQUE (user_id, product_id);


--
-- Name: idx_attendance_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_attendance_user ON public.attendance USING btree (user_id);


--
-- Name: idx_audit_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_action ON public.audit_logs USING btree (action);


--
-- Name: idx_audit_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_user ON public.audit_logs USING btree (user_id);


--
-- Name: idx_banners_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_banners_active ON public.banners USING btree (active, "position");


--
-- Name: idx_booking_engineer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_booking_engineer ON public.service_bookings USING btree (engineer_id);


--
-- Name: idx_booking_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_booking_status ON public.service_bookings USING btree (status);


--
-- Name: idx_booking_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_booking_user ON public.service_bookings USING btree (user_id);


--
-- Name: idx_compliance_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compliance_status ON public.compliance_documents USING btree (status);


--
-- Name: idx_customer_notes_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_customer_notes_customer ON public.customer_notes USING btree (customer_id);


--
-- Name: idx_device_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_device_user ON public.device_tokens USING btree (user_id);


--
-- Name: idx_loyalty_txn_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_loyalty_txn_user ON public.loyalty_transactions USING btree (user_id);


--
-- Name: idx_notifications_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notifications_user ON public.notifications USING btree (user_id);


--
-- Name: idx_osh_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_osh_order ON public.order_status_history USING btree (order_id);


--
-- Name: idx_payouts_vendor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_payouts_vendor ON public.vendor_payouts USING btree (vendor_id);


--
-- Name: idx_product_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_product_status ON public.products USING btree (status);


--
-- Name: idx_product_views_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_product_views_user ON public.product_views USING btree (user_id, created_at);


--
-- Name: idx_redemption_coupon_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_redemption_coupon_user ON public.coupon_redemptions USING btree (coupon_id, user_id);


--
-- Name: idx_referral_referrer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_referral_referrer ON public.referrals USING btree (referrer_id);


--
-- Name: idx_refresh_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_token ON public.refresh_tokens USING btree (token);


--
-- Name: idx_returns_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_returns_order ON public.return_requests USING btree (order_id);


--
-- Name: idx_returns_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_returns_user ON public.return_requests USING btree (user_id);


--
-- Name: idx_reviews_product; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reviews_product ON public.reviews USING btree (product_id);


--
-- Name: idx_search_logs_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_search_logs_user ON public.search_logs USING btree (user_id);


--
-- Name: idx_service_item_cat; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_service_item_cat ON public.service_items USING btree (category_id);


--
-- Name: idx_translation_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_translation_lookup ON public.translations USING btree (entity_type, entity_id, locale);


--
-- Name: idx_txn_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_txn_order ON public.payment_transactions USING btree (order_id);


--
-- Name: idx_txn_payment; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_txn_payment ON public.payment_transactions USING btree (payment_id);


--
-- Name: idx_wh_inventory_product; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_wh_inventory_product ON public.warehouse_inventory USING btree (product_id);


--
-- Name: idx_wtxn_wallet; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_wtxn_wallet ON public.wallet_transactions USING btree (wallet_id);


--
-- Name: addresses fk1fa36y2oqhao3wgg2rw1pi459; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.addresses
    ADD CONSTRAINT fk1fa36y2oqhao3wgg2rw1pi459 FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: custom_role_permissions fk649yjpx10ga15mbk5wvwehdvi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_role_permissions
    ADD CONSTRAINT fk649yjpx10ga15mbk5wvwehdvi FOREIGN KEY (permission_id) REFERENCES public.permissions(id);


--
-- Name: blog_post_tags fk7tf83uatm3u6lm9mcthgr8vfh; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.blog_post_tags
    ADD CONSTRAINT fk7tf83uatm3u6lm9mcthgr8vfh FOREIGN KEY (post_id) REFERENCES public.blog_posts(id);


--
-- Name: user_custom_roles fk8n7j4uclgcmrtfn5cx6oq08h7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_custom_roles
    ADD CONSTRAINT fk8n7j4uclgcmrtfn5cx6oq08h7 FOREIGN KEY (custom_role_id) REFERENCES public.custom_roles(id);


--
-- Name: user_custom_roles fk8rx2hpulrfq3f19b0p6qufkgb; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_custom_roles
    ADD CONSTRAINT fk8rx2hpulrfq3f19b0p6qufkgb FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: permissions fk8x2y1lngvikyd3o96y7fmsh88; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.permissions
    ADD CONSTRAINT fk8x2y1lngvikyd3o96y7fmsh88 FOREIGN KEY (group_id) REFERENCES public.permission_groups(id);


--
-- Name: products fka3a4mpsfdf4d2y6r8ra3sc8mv; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fka3a4mpsfdf4d2y6r8ra3sc8mv FOREIGN KEY (brand_id) REFERENCES public.brands(id);


--
-- Name: order_items fkbioxgbv59vetrxe0ejfubep1w; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.order_items
    ADD CONSTRAINT fkbioxgbv59vetrxe0ejfubep1w FOREIGN KEY (order_id) REFERENCES public.orders(id);


--
-- Name: user_roles fkhfh9dx7w3ubf1co1vdev94g3f; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_roles
    ADD CONSTRAINT fkhfh9dx7w3ubf1co1vdev94g3f FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: products fkog2rp4qthbtt2lfyhfo32lsw9; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fkog2rp4qthbtt2lfyhfo32lsw9 FOREIGN KEY (category_id) REFERENCES public.categories(id);


--
-- Name: product_variants fkosqitn4s405cynmhb87lkvuau; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_variants
    ADD CONSTRAINT fkosqitn4s405cynmhb87lkvuau FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- Name: cart_items fkpcttvuq4mxppo8sxggjtn5i2c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cart_items
    ADD CONSTRAINT fkpcttvuq4mxppo8sxggjtn5i2c FOREIGN KEY (cart_id) REFERENCES public.carts(id);


--
-- Name: custom_role_permissions fkpgpldh98tkpfin3wtw14c1wq3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.custom_role_permissions
    ADD CONSTRAINT fkpgpldh98tkpfin3wtw14c1wq3 FOREIGN KEY (custom_role_id) REFERENCES public.custom_roles(id);


--
-- Name: machine_images fkq2mr5282u7bgmy961m0mcuvr4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.machine_images
    ADD CONSTRAINT fkq2mr5282u7bgmy961m0mcuvr4 FOREIGN KEY (machine_id) REFERENCES public.machines(id);


--
-- Name: product_images fkqnq71xsohugpqwf3c9gxmsuy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.product_images
    ADD CONSTRAINT fkqnq71xsohugpqwf3c9gxmsuy FOREIGN KEY (product_id) REFERENCES public.products(id);


--
-- PostgreSQL database dump complete
--
