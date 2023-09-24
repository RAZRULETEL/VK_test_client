package com.mastik.vk_test_mod.dataTypes;

public class RepostCounters {
    private final int count, wallCount, mailCount, userReposted;

    public RepostCounters(int count, int wallCount, int mailCount, int userReposted) {
        this.count = count;
        this.wallCount = wallCount;
        this.mailCount = mailCount;
        this.userReposted = userReposted;
    }

    public int getCount() {
        return count;
    }

    public int getWallCount() {
        return wallCount;
    }

    public int getMailCount() {
        return mailCount;
    }

    public int getUserReposted() {
        return userReposted;
    }
}
