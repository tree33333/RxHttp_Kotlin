package per.goweii.rxhttp.kt.download.exception

import java.lang.RuntimeException

/**
 * <p>文件描述：<p>
 * <p>@author 烤鱼<p>
 * <p>@date 2020/1/1 0001 <p>
 * <p>@update 2020/1/1 0001<p>
 * <p>版本号：1<p>
 *
 */
class RangeLengthIsZeroException:RuntimeException("断点处请求长度为0") {
}