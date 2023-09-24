package com.mastik.vk_test_mod.dataTypes.attachments;

public enum PhotoSize implements Comparable<PhotoSize> {
    s(75), m(130), x(604), o(130, true), p(200, true), q(320, true), r(510, true), y(807), z(1080), w(2560);
    private static final PhotoSize[] sortedValues;
    private final int maxSize;
    private final boolean cropping;

    static {
        sortedValues = PhotoSize.values();
        for (int i = 0; i < sortedValues.length - 1; i++)//Bubble sort ^_^
            for (int l = 0; l < sortedValues.length - i; l++)
                if (sortedValues[i].getMaxSideSize() > sortedValues[i + 1].getMaxSideSize()) {
                    PhotoSize buff = sortedValues[i];
                    sortedValues[i] = sortedValues[i + 1];
                    sortedValues[i + 1] = buff;
                } else if (sortedValues[i].getMaxSideSize() == sortedValues[i + 1].getMaxSideSize())
                    if (sortedValues[i].isCropping() && !sortedValues[i + 1].isCropping()) {
                        PhotoSize buff = sortedValues[i];
                        sortedValues[i] = sortedValues[i + 1];
                        sortedValues[i + 1] = buff;
                    }
    }

    PhotoSize(int maxSize) {
        this(maxSize, false);
    }

    PhotoSize(int maxSize, boolean cropping) {
        this.maxSize = maxSize;
        this.cropping = cropping;
    }

    public int getMaxSideSize() {
        return maxSize;
    }

    public boolean isCropping() {
        return cropping;
    }

    public static PhotoSize getClosest(int width){
        return getClosest(width, true, false);
    }

    public static PhotoSize getClosest(int width, boolean ignoreCropping, boolean isCroppedRequired) {
        PhotoSize lastLower = s;
        for (PhotoSize size : PhotoSize.sortedValues()) {
            if (size.getMaxSideSize() > width && ignoreCropping || isCroppedRequired == size.isCropping()) {
                if (width - lastLower.getMaxSideSize() > size.getMaxSideSize() - width)
                    return size;
                else
                    return lastLower;
            }
            lastLower = size;
        }
        return ignoreCropping ? w : isCroppedRequired ? r : w;
    }

    public static PhotoSize[] sortedValues() {
        return sortedValues;
    }

    public PhotoSize getNext() {
        return getNext(false);
    }

    public PhotoSize getNext(boolean ignoreCropping) {
        if(!ignoreCropping && this == r)
            return y;
        boolean found = false;
        for (PhotoSize size : sortedValues) {
            if (found && ignoreCropping || size.cropping == this.cropping)
                return size;
            if(this == size)
                found = true;
        }

        throw new IllegalStateException("There is no bigger size");
    }

}
