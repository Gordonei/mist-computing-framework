#!/usr/bin/env python3

import http.server
import logging
import random
import time
import hashlib
import quotes
from urllib.parse import urlparse, parse_qs

PORT = 8000

REDIRCT_URL = "https://twitter.com/Gord1ei/status/830314829063843840"


class GetHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        http.server.SimpleHTTPRequestHandler.end_headers(self)

    def do_GET(self):
        logging.info(self)

        # Response Code
        self.send_response(200)

        # Headers
        self.send_header('Content-type', 'text/html')
        self.end_headers()

        query_components = parse_qs(urlparse(self.path).query)

        # Content of Response
        response_message = None
        if ("Response" in query_components):
            logging.debug("Checking response")
            client_response = query_components["Response"][0]
            client_id = query_components["RequestId"][0]
            logging.debug("Received response: %s (%s)" % (client_response, client_id))
            logging.debug("  Actual response: %s" % (
            hashlib.sha256(quotes.RANDOM_QUOTES()[int(client_id)].encode("utf-8")).hexdigest()))

            time.sleep(5)
            response_message = (u'%s' % REDIRCT_URL).encode("utf-8")
        else:
            choice = random.randint(0, len(quotes.RANDOM_QUOTES()))
            quote = quotes.RANDOM_QUOTES()[choice]
            logging.debug("Asking to encode %s (%d)" % (quote, choice))
            response_message = (u'%s\n%d' % (quotes.RANDOM_QUOTES()[choice], choice)).encode("utf-8")
        self.wfile.write(response_message)

        return


httpd = http.server.HTTPServer(("", PORT), GetHandler)

httpd.serve_forever()
