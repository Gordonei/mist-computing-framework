import os
import random

import tweepy

URLS = 100

# status.entities['urls']

class TwitterUrlGetter:

    LIST = "Gord1ei/news-journos"

    def __init__(self):
        CONSUMER_KEY = os.environ["TWITTER_CONSUMER_KEY"]
        CONSUMER_SECRET = os.environ["TWITTER_CONSUMER_SECRET"]
        ACCESS_KEY = os.environ["TWITTER_ACCESS_KEY"]
        ACCESS_SECRET = os.environ["TWITTER_ACCESS_SECRET"]

        auth = tweepy.OAuthHandler(CONSUMER_KEY, CONSUMER_SECRET)
        auth.set_access_token(ACCESS_KEY, ACCESS_SECRET)

        self.api = tweepy.API(auth,wait_on_rate_limit=True,wait_on_rate_limit_notify=True)
        self.url_sets = {}

    def get_url_set(self,list_name):
        # getting the URL list
        url_set = self.url_sets.get(list_name,set([]))

        # Adding more URLs if less than the threshold
        if len(url_set) < URLS: url_set |= set(self.get_urls_from_twitter(list_name))
        self.url_sets[list_name] = url_set

        return url_set

    def get_url(self, list_name):
        url_set = self.get_url_set(list_name)

        # Choosing a URL at random
        url = random.choice(list(url_set))

        # Removing the URL
        self.url_sets[list_name] -= {url}

        return url

    def get_urls_from_twitter(self, list_name):
        urls = []
        for page in tweepy.Cursor(self.api.search,"list:%s exclude:replies exclude:retweets"%(list_name)).pages(1):
            for status in page:
                urls += [url['expanded_url']
                         for url in status.entities['urls']
                         if "twitter" not in url['expanded_url']]

        return urls