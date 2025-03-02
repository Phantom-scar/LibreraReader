package org.ebookdroid.droids.mupdf.codec;

import com.foobnix.LibreraBuildConfig;
import com.foobnix.android.utils.LOG;
import com.foobnix.ext.Fb2Extractor;
import com.foobnix.model.AppState;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.core.codec.OutlineLink;

import java.util.ArrayList;
import java.util.List;

public class MuPdfOutline {

    private static final float[] temp = new float[4];
    public static final String EPUB_3_PAGES = "Epub3 pages";

    private long docHandle;

    private static native byte[] getTitle(long dochandle, long outlinehandle);

    private static native String getLink(long outlinehandle, long dochandle);

    private static native String getLinkUri(long outlinehandle, long dochandle);

    private static native int fillLinkTargetPoint(long outlinehandle, float[] point);

    private static native long getNext(long outlinehandle);

    private static native long getChild(long outlinehandle);

    private static native long open(long dochandle);

    private static native void free(long dochandle);

    public List<OutlineLink> getOutline(final long dochandle) {
        final List<OutlineLink> ls = new ArrayList<OutlineLink>();
        docHandle = dochandle;
        TempHolder.lock.lock();
        try {


            final long outline = open(dochandle);
            ttOutline(ls, outline, 0);
            free(dochandle);

            ls.add(new OutlineLink("", "", -1, dochandle, ""));
        } finally {
            TempHolder.lock.unlock();
        }

        if (AppState.get().isShowPageNumbers && !Fb2Extractor.epub3Pages.isEmpty()) {
            ls.add(new OutlineLink(EPUB_3_PAGES, "", 0, docHandle, ""));
            for (int i : Fb2Extractor.epub3Pages.keySet()) {
                ls.add(new OutlineLink("Page " + i, Fb2Extractor.epub3Pages.get(i), 1, docHandle, Fb2Extractor.epub3Pages.get(i)));
            }
        }

        return ls;
    }

    private void ttOutline(final List<OutlineLink> ls, long outline, final int level) {
        while (outline != -1) {
            byte[] res = getTitle(docHandle, outline);
            if (res == null) {
                break;
            }
            String title = new String(res);
            String linkUri = getLinkUri(outline, docHandle);


            final String link = getLink(outline, docHandle);

            LOG.d("linkUri", linkUri, title, link);

            if (LibreraBuildConfig.DEBUG && linkUri != null) {
                title = title + "[" + linkUri + "]";


            }

            if (title != null) {
                final OutlineLink outlineLink = new OutlineLink(title, link, level, docHandle, linkUri);

                boolean toAdd = true;
                if (AppState.get().outlineMode == AppState.OUTLINE_ONLY_HEADERS) {
                    if (outlineLink.getTitle().contains("[subtitle]")) {
                        toAdd = false;
                    }
                }
                outlineLink.setTitle(outlineLink.getTitle().replace("[title]", "").replace("[subtitle]", ""));

                if (outlineLink.getTitle().contains(Fb2Extractor.DIVIDER)) {
                    try {
                        String[] split = outlineLink.getTitle().split(Fb2Extractor.DIVIDER);
                        int level2 = Integer.parseInt(split[0]);
                        outlineLink.setLevel(level2);
                        outlineLink.setTitle(split[1]);
                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }

                if (toAdd) {
                    ls.add(outlineLink);
                }
            }

            final long child = getChild(outline);
            ttOutline(ls, child, level + 1);

            outline = getNext(outline);
        }
    }
}
