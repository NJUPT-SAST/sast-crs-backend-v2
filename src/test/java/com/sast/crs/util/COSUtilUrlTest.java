package com.sast.crs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class COSUtilUrlTest {

    @Test
    void extractsCosObjectNameWhenFilenameContainsSpace() {
        String url = "https://bucket.cos.ap-shanghai.myqcloud.com/dev-local-private/11/work/4/file (1).pdf";

        assertEquals("11/work/4/file (1).pdf", COSUtil.getObjectNameCOS(url));
    }

    @Test
    void extractsCosObjectNameWhenFilenameIsEncoded() {
        String url = "https://bucket.cos.ap-shanghai.myqcloud.com/dev-local-private/11/work/4/file%20(1).pdf";

        assertEquals("11/work/4/file (1).pdf", COSUtil.getObjectNameCOS(url));
    }

    @Test
    void preservesPlusInFilename() {
        String url = "https://bucket.cos.ap-shanghai.myqcloud.com/dev-local-private/11/work/4/c++-guide.pdf";

        assertEquals("11/work/4/c++-guide.pdf", COSUtil.getObjectNameCOS(url));
    }

    @Test
    void extractsCosObjectNameWhenFilenameContainsBrackets() {
        String url = "https://bucket.cos.ap-shanghai.myqcloud.com/dev-local-private/11/work/4/申报书 (1)[1].pdf";

        assertEquals("11/work/4/申报书 (1)[1].pdf", COSUtil.getObjectNameCOS(url));
    }

    @Test
    void ignoresPresignedQueryString() {
        String url = "https://bucket.cos.ap-shanghai.myqcloud.com/dev-local-private/11/work/4/file.pdf" + "?q-sign-algorithm=sha1&q-ak=AKIDEXAMPLE&q-sign-time=1;2";

        assertEquals("11/work/4/file.pdf", COSUtil.getObjectNameCOS(url));
    }

    /** URL 里可能残留裸 %（历史数据里的文件名本身带 %），解析不能因此失败 */
    @Test
    void keepsRawPercentWhenItIsNotAValidEscape() {
        String url = "https://bucket.cos.ap-shanghai.myqcloud.com/dev-local-private/11/work/4/50%off.pdf";

        assertEquals("11/work/4/50%off.pdf", COSUtil.getObjectNameCOS(url));
    }

    /** 对象名不做转义（file.url 要按原样查库），所以读写必须一字不差 */
    @Test
    void keepsAwkwardCharactersInObjectName() {
        String url = "https://bucket.cos.ap-shanghai.myqcloud.com/dev-local-private/11/work/4/" + "申报书-4d844c1a-2026软研笔试题-试卷 (1)#[a].pdf";

        assertEquals("11/work/4/申报书-4d844c1a-2026软研笔试题-试卷 (1)#[a].pdf", COSUtil.getObjectNameCOS(url));
    }

    /** isBucketURL 不能再依赖 URI 解析，否则带空格的文件会被误判成普通文本 */
    @Test
    void recognisesBucketUrlWithAwkwardFilename() {
        COSUtil cosUtil = new COSUtil("id", "key", "ap-shanghai", 15, 15, "bucket", "crs-public", "crs-private");

        assertTrue(cosUtil.isBucketURL("https://bucket.cos.ap-shanghai.myqcloud.com/crs-private/11/work/4/申报书 (1).pdf"));
        assertTrue(cosUtil.isBucketURL("https://bucket.cos.ap-shanghai.myqcloud.com/crs-private/11/work/4/a.pdf?q-sign-time=1;2"));
        assertFalse(cosUtil.isBucketURL("https://evil.example.com/crs-private/11/work/4/a.pdf"));
        assertFalse(cosUtil.isBucketURL("就是一段普通文本"));
        assertFalse(cosUtil.isBucketURL(""));
    }
}
