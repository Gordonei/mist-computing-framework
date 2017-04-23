import os
import time
import threading

import tweepy

URLS = 50
REFRESH_DELAY = 24*60*60

class UrlSetSpindle(threading.Thread):
    def __init__(self,list_name,logger,api):
        threading.Thread.__init__(self)

        self.list_name = list_name
        self.logger = logger
        self.api = api

        self.url_lock = threading.Lock()
        self.url_set = set([])
        self.retrieval_time = time.time()

    def run(self):
        self.logger.debug("Starting thread for %s" % self.list_name)
        while True:
            time_since_retrieval = time.time() - self.retrieval_time
            if len(self.url_set) < URLS or time_since_retrieval > REFRESH_DELAY :
                self.update_url_set()
            else:
                time.sleep(10)
                self.logger.debug("Thread sleeping, %d URLs in list"%len(self.url_set))

    def update_url_set(self):
        self.logger.debug("Retrieving more URLs for %s."%self.list_name)

        self.url_lock.acquire()

        self.url_set |= set(self.get_urls_from_twitter())
        self.retrieval_time = time.time()

        # If we've gotten too big, trim things down
        while(len(self.url_set) > 5*URLS):
            self.url_set.pop()

        self.logger.debug("%d URLs in %s."%(len(self.url_set),self.list_name))

        self.url_lock.release()

    def get_url(self):
        # Choosing a URL at random
        self.logger.debug("Choosing a URL at random")

        self.url_lock.acquire()
        url = self.url_set.pop()
        self.url_lock.release()

        # Serving up
        self.logger.debug("Serving up %s"%url)

        return url

    def get_urls_from_twitter(self):
        urls = []
        for page in tweepy.Cursor(self.api.search,
                                  "list:%s exclude:replies exclude:retweets"%(self.list_name)).pages(10):
            for status in page:
                urls += [url['expanded_url']
                         for url in status.entities['urls']
                         if "twitter" not in url['expanded_url']]

        # Updating retrieval time
        self.retrieve_time = time.time()

        return urls

class TwitterUrlGetter:
    def __init__(self,logger):
        self.logger = logger
        self.logger.debug("Initialising Twitter URL list.")

        CONSUMER_KEY = os.environ["TWITTER_CONSUMER_KEY"]
        CONSUMER_SECRET = os.environ["TWITTER_CONSUMER_SECRET"]
        ACCESS_KEY = os.environ["TWITTER_ACCESS_KEY"]
        ACCESS_SECRET = os.environ["TWITTER_ACCESS_SECRET"]

        auth = tweepy.OAuthHandler(CONSUMER_KEY, CONSUMER_SECRET)
        auth.set_access_token(ACCESS_KEY, ACCESS_SECRET)

        self.api = tweepy.API(auth,wait_on_rate_limit=True,wait_on_rate_limit_notify=True)
        self.url_spindles = {}

    def get_url_spindle(self,list_name):
        url_spindle = self.url_spindles.get(list_name,
                                            UrlSetSpindle(list_name,self.logger,self.api))

        if(list_name not in self.url_spindles):
            self.url_spindles[list_name] = url_spindle

        return url_spindle

    def get_url(self,list_name):
        url_spindle = self.get_url_spindle(list_name)

        if not url_spindle.is_alive():
            url_spindle.start()

        return url_spindle.get_url()