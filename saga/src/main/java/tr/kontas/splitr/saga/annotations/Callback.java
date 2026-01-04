package tr.kontas.splitr.saga.annotations;

/**
 * Represents a callback configuration for a saga.
 * <p>
 * This annotation allows defining:
 * <ul>
 *     <li>The callback URL template, with optional placeholders for saga state fields.</li>
 *     <li>Optional HTTP headers to include in the callback request.</li>
 *     <li>Additional key-value pairs to include in the callback body.</li>
 *     <li>Fields or parameters that should be excluded from injection into headers or body.</li>
 * </ul>
 *
 * <p>Placeholders in the URL, headers, and body should correspond to fields in the saga state
 * or runtime-injectable values. Fields listed in {@link #dontInject()} will be ignored
 * during injection according to their specified target.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * &#64;Callback(
 *     url = "/api/callback/{orderId}/status",
 *     headers = {
 *         &#64;KeyValueItem(key = "Authorization", value = "Bearer {authToken}"),
 *         &#64;KeyValueItem(key = "Content-Type", value = "application/json")
 *     },
 *     additionalBodyItems = {
 *         &#64;KeyValueItem(key = "extraInfo1", value = "someValue1"),
 *         &#64;KeyValueItem(key = "extraInfo2", value = "someValue2")
 *     },
 *     dontInject = {
 *         &#64;DontInject(key = "sensitiveData", value = {DontInject.Target.BODY, DontInject.Target.HEADER}),
 *         &#64;DontInject(key = "authToken", value = {DontInject.Target.HEADER})
 *     }
 * )
 * </pre>
 *
 * <p>Notes:</p>
 * <ul>
 *     <li>Use placeholders in the format <code>{fieldName}</code> to inject saga state fields or runtime values.</li>
 *     <li>Fields not present in the saga state can be added via {@link #additionalBodyItems()}.</li>
 *     <li>{@link #dontInject()} can be used to prevent sensitive fields from being injected into headers, body, or both.</li>
 * </ul>
 */
public @interface Callback {

    /**
     * Callback URL template.
     * <p>
     * Use placeholders in the format <code>{parameterName}</code> to inject saga state or runtime values.
     * </p>
     *
     * @Example: "/api/callback/{orderId}/status"
     *
     * @return the callback URL template
     */
    String url() default "";

    /**
     * Optional HTTP headers for the callback request.
     *
     * @Example:
     * <pre>
     * &#64;Callback(
     *     url = "/api/callback/{orderId}/status",
     *     headers = {
     *         &#64;KeyValueItem(key = "Authorization", value = "Bearer {authToken}"),
     *         &#64;KeyValueItem(key = "Content-Type", value = "application/json")
     *     }
     * )
     * </pre>
     *
     * @return an array of {@link KeyValueItem} annotations representing HTTP headers
     */
    KeyValueItem[] headers() default {};

    /**
     * Specifies saga state fields or method parameter names that should not be injected
     * into the callback URL, headers, or body, along with the target.
     *
     * @Example:
     * <pre>
     * &#64;Callback(
     *     url = "/api/callback/{orderId}/status",
     *     dontInject = {
     *         &#64;DontInject(key = "sensitiveData", value = {DontInject.Target.BODY, DontInject.Target.HEADER}),
     *         &#64;DontInject(key = "authToken", value = {DontInject.Target.HEADER})
     *     }
     * )
     * </pre>
     *
     * @return an array of {@link DontInject} annotations representing fields to exclude
     */
    DontInject[] dontInject() default {};

    /**
     * Additional key-value pairs for the callback request body.
     * <p>
     * These are useful for adding metadata or contextual fields that are not present
     * in the saga state.
     * </p>
     *
     * @Example:
     * <pre>
     * &#64;Callback(
     *     url = "/api/callback/{orderId}/status",
     *     additionalBodyItems = {
     *         &#64;KeyValueItem(key = "extraInfo1", value = "someValue1"),
     *         &#64;KeyValueItem(key = "extraInfo2", value = "someValue2")
     *     }
     * )
     * </pre>
     *
     * @return an array of {@link KeyValueItem} annotations representing additional body items
     */
    KeyValueItem[] additionalBodyItems() default {};
}
