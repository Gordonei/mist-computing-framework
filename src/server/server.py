#!/usr/bin/env python3

# Built-ins
import http.server
import logging
import random
import time
import hashlib
from urllib.parse import urlparse, parse_qs
import json

# Mine
import quotes
import twitter_url_getter

PORT = 8000


class GetHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, PUT, POST, OPTIONS')
        self.send_header("Access-Control-Allow-Headers", "X-Requested-With, Content-Type")

        http.server.SimpleHTTPRequestHandler.end_headers(self)

        return

    def do_OPTIONS(self):
        self.send_response(200, "ok")

        self.end_headers()

        return

    def do_GET(self):
        # Response Code
        self.send_response(200)

        # Headers
        self.send_header('Content-type', 'text/html')
        self.end_headers()

        choice = random.randint(0, len(quotes.RANDOM_QUOTES()))
        quote = quotes.RANDOM_QUOTES()[choice]
        logger.debug("Asking to encode %s (%d)" % (quote, choice))

        #response_message = (u'%s\n%d' % (quotes.RANDOM_QUOTES()[choice], choice)).encode("utf-8")
        response_dict = {"payload":quotes.RANDOM_QUOTES()[choice],
                         "wid":str(choice),
                         "cid":str(0),
                         "aid":"sha256"}

        response_message = json.dumps(response_dict)

        """
        query_components = parse_qs(urlparse(self.path).query)

        # Content of Response
        response_message = None
        if ("Response" in query_components):
            logger.debug("Checking response")
            client_response = query_components["Response"][0]
            client_id = query_components["RequestId"][0]
            logger.debug("Received response: %s (%s)" % (client_response, client_id))
            logger.debug("  Actual response: %s" % (
            hashlib.sha256(quotes.RANDOM_QUOTES()[int(client_id)].encode("utf-8")).hexdigest()))

            redirect_url = url_getter.get_url("Gord1ei/news-journos")
            response_message = (u'%s' % redirect_url).encode("utf-8")
            #time.sleep(1)
        else:
            choice = random.randint(0, len(quotes.RANDOM_QUOTES()))
            quote = quotes.RANDOM_QUOTES()[choice]
            logger.debug("Asking to encode %s (%d)" % (quote, choice))
            response_message = (u'%s\n%d' % (quotes.RANDOM_QUOTES()[choice], choice)).encode("utf-8")
        """

        self.wfile.write(response_message.encode("utf-8"))

        return

    def do_PUT(self):
        # Response Code
        self.send_response(200)

        # Headers
        self.send_header('Content-type', 'text/html')
        self.end_headers()

        length = int(self.headers['Content-Length'])
        content = self.rfile.read(length)
        response = json.loads(content.decode("utf-8"))

        logger.debug("Checking response")
        wid = int(response["wid"])
        check = hashlib.sha256(quotes.RANDOM_QUOTES()[int(wid)].encode("utf-8")).hexdigest()

        redirect_url = url_getter.get_url("Gord1ei/news-journos")
        response_message = redirect_url.encode("utf-8")

        self.wfile.write(response_message)

        return

logger = logging.getLogger('server.py')
logger.setLevel("DEBUG")
FORMAT = '%(asctime)-15s %(message)s'
logging.basicConfig(format=FORMAT)

url_getter = twitter_url_getter.TwitterUrlGetter()
httpd = http.server.HTTPServer(("", PORT), GetHandler)

httpd.serve_forever()
