#!/usr/bin/env python3
"""GitCode Release 附件上传：读 upload_url 响应 JSON，PUT 二进制（headers 全带）。

用法: python3 scripts/gitcode-upload.py <本地文件> <upload_url 响应 JSON 文件>

响应 headers（x-obs-* 等）参与 OBS 预签名校验，必须原样全部带上。
供 CI（.github/workflows/android-release.yml）调用；sync-gitcode-releases.py 内实现相同逻辑。
"""

import json
import sys
import urllib.request


def main() -> int:
    if len(sys.argv) != 3:
        print("用法: gitcode-upload.py <file> <upload_url.json>", file=sys.stderr)
        return 2
    path, meta = sys.argv[1], sys.argv[2]
    info = json.load(open(meta))
    url = info["url"]
    headers = {k: str(v) for k, v in (info.get("headers") or {}).items()}
    with open(path, "rb") as f:
        data = f.read()
    req = urllib.request.Request(url, data=data, method="PUT", headers=headers)
    with urllib.request.urlopen(req, timeout=600) as r:
        r.read()
    return 0


if __name__ == "__main__":
    sys.exit(main())