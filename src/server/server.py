#!/usr/bin/env python3

# Built-ins
import http.server
import logging
import random
import json

# Mine
import quotes
import twitter_url_getter

PORT = 8000

DIFFICULTY = 100

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

        choice = random.randint(0, len(quotes.RANDOM_QUOTES())-1)
        quote,check = quotes.RANDOM_QUOTES()[choice]
        logger.debug("Asking to encode %s (%d)" % (quote, choice))

        response_dict = {"payload":quote,
                         "wid":str(choice),
                         "cid":str(0),
                         "aid":"sha256"}

        response_message = json.dumps(response_dict)

        self.wfile.write(response_message.encode("utf-8"))

        return

    def do_PUT(self):
        # Checking content
        length = int(self.headers['Content-Length'])
        content = self.rfile.read(length)
        response = json.loads(content.decode("utf-8"))
        self.log_message("Received response: %s",response["payload"])

        logger.debug("Checking response")
        wid = int(response["wid"])
        quote,check = quotes.RANDOM_QUOTES()[wid]

        self.log_message(" Correct response: %s",check)
        if(check == response["payload"].encode("utf-8")):
            self.log_message("MISMATCH!")

        # Response Code
        # 204 means try again
        # 200 means redirect link included
        retry = random.randint(0,DIFFICULTY) < DIFFICULTY - 1

        if(retry):
            self.send_response(204)
        else:
            self.send_response(200)

        # Headers
        self.send_header('Content-type', 'text/html')
        self.end_headers()

        if not(retry):
            redirect_url = url_getter.get_url("Gord1ei/news-journos")
            response_message = redirect_url.encode("utf-8")
            print("response message:",response_message)
            self.wfile.write(response_message)

        return

logger = logging.getLogger('server.py')
logger.setLevel("DEBUG")
FORMAT = '%(asctime)-15s %(message)s'
logging.basicConfig(format=FORMAT)

url_getter = twitter_url_getter.TwitterUrlGetter(logger)
url_getter.get_url("Gord1ei/news-journos")

httpd = http.server.HTTPServer(("", PORT), GetHandler)

httpd.serve_forever()
