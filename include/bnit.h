/*
 * bnit.h — C API for the restored Lane request-signing algorithm
 * (libbnit.so / BNITManager.cdrl).
 *
 * Link against liblaned_bnit.a / liblaned_bnit.so / laned_bnit.dll produced by
 * `cargo build -p laned-bnit`.
 *
 *   bnit_headers h;
 *   if (bnit_build("GET", "/user/playlists", "", NULL, 0, 0, &h) == 0) {
 *       // h.core_token   -> "X-Core-Token:<value>"
 *       // h.trace_id     -> "X-Request-Trace-Id:<uuid>"
 *       // h.client_meta  -> "X-Client-Meta:<base64>"
 *       // for POST: send h.body / h.body_len as the request body
 *       bnit_headers_free(&h);
 *   }
 */
#ifndef BNIT_H
#define BNIT_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct bnit_headers {
    char *core_token;      /* "X-Core-Token:<value>", NUL-terminated          */
    char *trace_id;        /* "X-Request-Trace-Id:<uuid>"                     */
    char *client_meta;     /* "X-Client-Meta:<base64>"                        */
    uint8_t *body;         /* encrypted request body (POST), may be NULL      */
    size_t body_len;       /* 0 when there is no body                         */
} bnit_headers;

/*
 * Build the signature headers for a request.
 *
 * method    "GET", "POST", ...
 * path      "/user/playlists" (no query)
 * query     "page=1&pageSize=50" or "" (no leading '?')
 * body      plaintext request body bytes (NULL for none)
 * body_len  length of body
 * timestamp offset added to the current time in ms; 0 means "now"
 * out       caller-allocated struct, filled on success
 *
 * Returns 0 on success, -1 on invalid arguments, -2 on internal error.
 * Free the result with bnit_headers_free().
 */
int bnit_build(const char *method,
               const char *path,
               const char *query,
               const uint8_t *body,
               size_t body_len,
               int64_t timestamp,
               bnit_headers *out);

/* Release everything allocated by bnit_build(). Safe to call on zeroed data. */
void bnit_headers_free(bnit_headers *h);

/* Library version, static storage. */
const char *bnit_version(void);

#ifdef __cplusplus
}
#endif

#endif /* BNIT_H */
