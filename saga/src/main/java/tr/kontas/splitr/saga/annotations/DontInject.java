package tr.kontas.splitr.saga.annotations;

/**
 * Specifies that a saga state field or method parameter should not be injected into
 * certain parts of the callback request.
 */
public @interface DontInject {

    /**
     * The field or parameter name to exclude from injection.
     */
    String key();

    /**
     * Where the field should be excluded from.
     * @see DontInject.Target
     */
    Target[] value() default {Target.BODY, Target.HEADER};

    /**
     * Enumeration of possible injection targets to exclude.
     *
     * <ul>BODY - Exclude from the request body.</ul>
     * <ul>HEADER - Exclude from the request headers.</ul>
     * <ul>ALL - Exclude from all of the above.</ul>
     *
     * @see DontInject
     */
    enum Target {
        BODY,
        HEADER,
        ALL // ALL is kept for backward compatibility, but it's recommended to use BODY and HEADER explicitly.
    }
}
