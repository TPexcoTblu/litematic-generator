# Format size check — 2026-11-03

The same hollow sphere positions and air/smooth-stone palette were encoded as the
old structure NBT representation and the new Litematic v7 representation. Both
used GZIP compression level 6. The legacy NBT byte stream was reconstructed from
the original exporter layout; this is a file-size comparison, not a timing benchmark.

| Radius | Non-air blocks | Structure `.nbt` | `.litematic` | Reduction |
| ---: | ---: | ---: | ---: | ---: |
| 16 | 3,338 | 8,422 B | 1,889 B | 77.6% |
| 32 | 12,606 | 32,624 B | 6,285 B | 80.7% |
| 64 | 51,554 | 135,485 B | 31,603 B | 76.7% |

The new representation removes per-block position compounds and stores two-bit
palette indices. Results depend on geometry and bounding volume. Metadata includes
timestamps, so repeated compressed sizes may vary slightly.

The raw files and JSON results from this local check are in `build/format-benchmark/`.
Independent NBT decoding and geometry checks are implemented in `tests/test_litematic.py`.
