# code_quality_audit 测试夹具说明（解压回归）

本目录用于存放代码质量治理任务相关的可复现测试夹具说明。

## 1. SevenZip 解压回归夹具（G-T0067 / REPO_SEVEN_ZIP-T004）

### 1.1 存放位置

- 测试资产：`core_storage_component/src/androidTest/assets/seven_zip/`
  - `chunked_payload.zip`
  - `chunked_payload.7z`

> 说明：资产放在 `androidTest/assets` 便于 instrumentation 用例直接读取；本 README 记录生成方式与校验信息，满足可复现要求。

### 1.2 覆盖目标

- 覆盖格式：`zip`、`7z`
- 覆盖场景：
  - 多文件解压
  - 大文件输出（`payload_chunked.bin`，1 MiB）
  - “多次 write”语义回归（通过 hash/size 校验验证流式写入未覆盖）

### 1.3 期望校验（固定夹具）

- `payload_chunked.bin`
  - `size=1048576`
  - `sha256=1612586a56503d400b5796768f9ce3bde548b001d80a07f2d5bb0a45c98fec09`
- `alpha.txt`
  - `size=68`
  - `sha256=f63deadcaa4e1fc1891530ee680b23c24829f944cf6a09a82c60a7a24541f246`
- `beta.json`
  - `size=74`
  - `sha256=c81148c0086138b35d3bd3894ebc241f41a659a8a154e26c6ba65c3dae3285ed`

### 1.4 生成方式（可复现）

可使用以下脚本生成同一批夹具（固定随机种子 `20260206`）：

```bash
python3 - <<'PY'
import json, pathlib, random, hashlib, zipfile
import py7zr

root = pathlib.Path('.tmp/seven_zip_fixture_gen')
root.mkdir(parents=True, exist_ok=True)
files_dir = root / 'files'
files_dir.mkdir(exist_ok=True)

rnd = random.Random(20260206)
payload = bytes(rnd.getrandbits(8) for _ in range(1024 * 1024))
(files_dir / 'payload_chunked.bin').write_bytes(payload)
(files_dir / 'alpha.txt').write_text('DDPlayTV seven-zip fixture\n用于验证多次 write 输出完整性\n', encoding='utf-8')
(files_dir / 'beta.json').write_text(
    json.dumps({'feature': 'seven_zip', 'version': 1, 'note': 'instrumentation'}, ensure_ascii=False, indent=2) + '\n',
    encoding='utf-8',
)

with zipfile.ZipFile(root / 'chunked_payload.zip', 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zf:
    for name in ['payload_chunked.bin', 'alpha.txt', 'beta.json']:
        zf.write(files_dir / name, arcname=name)

with py7zr.SevenZipFile(root / 'chunked_payload.7z', 'w') as z:
    for name in ['payload_chunked.bin', 'alpha.txt', 'beta.json']:
        z.write(files_dir / name, arcname=name)

manifest = {}
for name in ['payload_chunked.bin', 'alpha.txt', 'beta.json']:
    data = (files_dir / name).read_bytes()
    manifest[name] = {'size': len(data), 'sha256': hashlib.sha256(data).hexdigest()}

(root / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('generated at', root)
PY
```

依赖：`py7zr`（仅用于生成 `7z` 夹具；运行 instrumentation 用例本身不依赖该包）。

### 1.5 对应用例

- `core_storage_component/src/androidTest/java/com/xyoye/common_component/utils/seven_zip/SevenZipUtilsInstrumentedTest.kt`
  - `extractZipAnd7zFixtures_matchExpectedHashAndSize`
  - `corruptedArchive_returnsNullAndCleansOutputDir`
  - `cancelExtraction_releasesResourcesAndAllowsCleanup`
