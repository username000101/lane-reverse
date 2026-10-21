#include "bnit.h"
#include <stdio.h>

int main(void) {
    bnit_headers h;
    int rc = bnit_build("GET", "/user/playlists", "", NULL, 0, 0, &h);
    if (rc != 0) { printf("bnit_build failed: %d\n", rc); return 1; }
    printf("%s\n%s\n%s\n", h.core_token, h.trace_id, h.client_meta);
    bnit_headers_free(&h);
    return 0;
}
