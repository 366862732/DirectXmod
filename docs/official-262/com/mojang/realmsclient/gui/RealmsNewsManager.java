/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.util.RealmsPersistence;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class RealmsNewsManager {
    private final RealmsPersistence newsLocalStorage;
    private boolean hasUnreadNews;
    private String newsLink;

    public RealmsNewsManager(RealmsPersistence newsLocalStorage) {
        this.newsLocalStorage = newsLocalStorage;
        RealmsPersistence.RealmsPersistenceData news = newsLocalStorage.read();
        this.hasUnreadNews = news.hasUnreadNews;
        this.newsLink = news.newsLink;
    }

    public boolean hasUnreadNews() {
        return this.hasUnreadNews;
    }

    public String newsLink() {
        return this.newsLink;
    }

    public void updateUnreadNews(RealmsNews newsResponse) {
        RealmsPersistence.RealmsPersistenceData news = this.updateNewsStorage(newsResponse);
        this.hasUnreadNews = news.hasUnreadNews;
        this.newsLink = news.newsLink;
    }

    private RealmsPersistence.RealmsPersistenceData updateNewsStorage(RealmsNews newsResponse) {
        RealmsPersistence.RealmsPersistenceData previousNews = this.newsLocalStorage.read();
        if (newsResponse.newsLink() == null || newsResponse.newsLink().equals(previousNews.newsLink)) {
            return previousNews;
        }
        RealmsPersistence.RealmsPersistenceData realmsNews = new RealmsPersistence.RealmsPersistenceData();
        realmsNews.newsLink = newsResponse.newsLink();
        realmsNews.hasUnreadNews = true;
        this.newsLocalStorage.save(realmsNews);
        return realmsNews;
    }
}

