"""Independent NBT decode and geometry checks. Run: python -m unittest discover -s tests -v"""
import gzip
import io
from pathlib import Path
import struct
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


def read_nbt(path):
    stream = io.BytesIO(gzip.decompress(path.read_bytes()))

    def unpack(fmt):
        return struct.unpack('>' + fmt, stream.read(struct.calcsize('>' + fmt)))[0]

    def string():
        return stream.read(unpack('H')).decode('utf-8')

    def payload(kind):
        if kind == 3:
            return unpack('i')
        if kind == 4:
            return unpack('q')
        if kind == 8:
            return string()
        if kind == 9:
            element, count = unpack('B'), unpack('i')
            return [payload(element) for _ in range(count)]
        if kind == 10:
            result = {}
            while (child := unpack('B')):
                key = string()
                result[key] = payload(child)
            return result
        if kind == 12:
            return [unpack('Q') for _ in range(unpack('i'))]
        raise AssertionError(f'Unexpected NBT type: {kind}')

    assert unpack('B') == 10
    assert string() == ''
    result = payload(10)
    assert stream.read() == b''
    return result


def occupied(root):
    region = root['Regions']['Generated']
    sx, sy, sz = (region['Size'][axis] for axis in 'xyz')
    data = region['BlockStates']
    return {(x, y, z) for y in range(sy) for z in range(sz) for x in range(sx)
            if (data[((y * sz + z) * sx + x) // 32]
                >> ((((y * sz + z) * sx + x) % 32) * 2)) & 3 == 1}


class LitematicTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.output = Path(cls.temp.name)
        sources = [*ROOT.glob('src/main/java/**/generators/*.java'),
                   *ROOT.glob('src/main/java/**/schematic/*.java'),
                   *ROOT.glob('src/test/java/**/CoreFixtures.java')]
        subprocess.run(['javac', '-encoding', 'UTF-8', '-d', str(cls.output),
                        *map(str, sources)], check=True)
        subprocess.run(['java', '-Djava.awt.headless=true', '-cp', str(cls.output),
                        'com.tpexcotblu.litematicgen.schematic.CoreFixtures', str(cls.output)], check=True)

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def test_word_boundary_and_axis_order(self):
        root = read_nbt(self.output / 'boundary.litematic')
        self.assertEqual(occupied(root), {(31, 0, 0), (32, 0, 0), (0, 1, 2)})

    def test_metadata_palette_and_lengths(self):
        for path in self.output.glob('*.litematic'):
            with self.subTest(path=path.name):
                root = read_nbt(path)
                self.assertEqual((root['Version'], root['SubVersion']), (7, 1))
                self.assertEqual(root['MinecraftDataVersion'], 4440)
                region = root['Regions']['Generated']
                self.assertEqual(region['BlockStatePalette'],
                                 [{'Name': 'minecraft:air'}, {'Name': 'minecraft:smooth_stone'}])
                self.assertEqual(root['Metadata']['TotalBlocks'], len(occupied(root)))
                self.assertEqual(len(region['BlockStates']), (root['Metadata']['TotalVolume'] + 31) // 32)
                for field in ('Entities', 'TileEntities', 'PendingBlockTicks', 'PendingFluidTicks'):
                    self.assertEqual(region[field], [])

    def test_sphere_and_circle_geometry(self):
        for name, axes in [('sphere_4', (0, 1, 2)), ('circle_4_horizontal', (0, 2)),
                           ('circle_4_vertical', (1, 2))]:
            root = read_nbt(self.output / (name + '.litematic'))
            expected = {(x, y, z) for x in range(9 if 0 in axes else 1)
                        for y in range(9 if 1 in axes else 1)
                        for z in range(9 if 2 in axes else 1)
                        if abs(sum(((x, y, z)[a] - 4) ** 2 for a in axes) ** 0.5 - 4) < 0.5}
            self.assertEqual(occupied(root), expected)

    def test_parabola_endpoints(self):
        self.assertTrue({(0, 0, 0), (4, 4, 0), (8, 0, 0)} <=
                        occupied(read_nbt(self.output / 'parabola_w8_h4.litematic')))
        self.assertTrue(occupied(read_nbt(self.output / 'parabola_w1_h3.litematic')))

    def test_formula_geometry_and_modes(self):
        filled = next(self.output.glob('formula_*_filled.litematic'))
        surface = next(self.output.glob('formula_*_surface.litematic'))
        ball = {(x, y, z) for x in range(9) for y in range(9) for z in range(9)
                if (x - 4) ** 2 + (y - 4) ** 2 + (z - 4) ** 2 <= 16}
        shell = {(x, y, z) for x in range(9) for y in range(9) for z in range(9)
                 if abs(((x - 4) ** 2 + (y - 4) ** 2 + (z - 4) ** 2) ** 0.5 - 4) <= 0.45}
        self.assertEqual(occupied(read_nbt(filled)), ball)
        self.assertEqual(occupied(read_nbt(surface)), shell)

    def test_text_filenames_are_safe(self):
        for path in self.output.glob('text_*.litematic'):
            self.assertTrue(path.name.isascii())
            self.assertLess(len(path.name), 96)
        empty = next(self.output.glob('text__12_0.litematic'))
        self.assertEqual(occupied(read_nbt(empty)), set())

    def test_no_temporary_files(self):
        self.assertEqual(list(self.output.glob('*.tmp')), [])


if __name__ == '__main__':
    unittest.main()
