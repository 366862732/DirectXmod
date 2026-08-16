import struct, sys

def read_utf8(data, pos):
    length = struct.unpack('>H', data[pos:pos+2])[0]
    return data[pos+2:pos+2+length].decode('utf-8', errors='replace'), pos+2+length

def parse_cp(data, pos, count):
    entries = {}
    i = 1
    while i < count and pos + 1 < len(data):
        tag = data[pos]
        if tag == 1:  # UTF8
            s, pos = read_utf8(data, pos+1)
            entries[i] = ('UTF8', s)
            i += 1
        elif tag == 7:  # CLASS
            name_idx = struct.unpack('>H', data[pos+1:pos+3])[0]
            entries[i] = ('CLASS', name_idx)
            pos += 3; i += 1
        elif tag == 9:  # FIELDREF
            cls = struct.unpack('>H', data[pos+1:pos+3])[0]
            nt = struct.unpack('>H', data[pos+3:pos+5])[0]
            entries[i] = ('FIELDREF', cls, nt)
            pos += 5; i += 1
        elif tag in (10, 11):  # METHODREF, INTERFACEMETHODREF
            pos += 5; i += 1
        elif tag == 12:  # NAME_AND_TYPE
            name_idx = struct.unpack('>H', data[pos+1:pos+3])[0]
            sig_idx = struct.unpack('>H', data[pos+3:pos+5])[0]
            entries[i] = ('NAME_AND_TYPE', name_idx, sig_idx)
            pos += 5; i += 1
        elif tag == 3:  # INT
            pos += 5; i += 1
        elif tag == 4:  # LONG
            pos += 9; i += 2
        elif tag in (5, 6):  # FLOAT, DOUBLE
            pos += 5 if tag == 5 else 9; i += 1
        elif tag == 8:  # STRING
            pos += 3; i += 1
        elif tag in (15, 16):  # METHOD_HANDLE, METHOD_TYPE
            pos += 3; i += 1
        elif tag == 18:  # CONSTANT_InvokeDynamic
            pos += 5; i += 1
        else:
            pos += 1; i += 1
    return entries

with open(sys.argv[1], 'rb') as f:
    data = f.read()

cp_count = struct.unpack('>H', data[8:10])[0]
entries = parse_cp(data, 10, cp_count)

# Find class info
minor_version = struct.unpack('>H', data[4:6])[0]
major_version = struct.unpack('>H', data[6:8])[0]
constant_pool_count = struct.unpack('>H', data[8:10])[0]
access_flags = struct.unpack('>H', data[10:12])[0]
this_class = struct.unpack('>H', data[12:14])[0]
super_class = struct.unpack('>H', data[14:16])[0]
interface_count = struct.unpack('>H', data[16:18])[0]

print(f'Java {major_version}.{minor_version}, this_class=#{this_class}, super_class=#{super_class}')
print()

# Parse fields
pos = 18
for _ in range(struct.unpack('>H', data[18:20])[0]):
    field_name_idx = struct.unpack('>H', data[pos:pos+2])[0]
    field_sig_idx = struct.unpack('>H', data[pos+2:pos+4])[0]
    field_name = entries.get(field_name_idx, ('?',''))[1] if field_name_idx in entries else '?'
    field_sig = entries.get(field_sig_idx, ('?',''))[1] if field_sig_idx in entries else '?'
    print(f'  Field #{field_name_idx}: {field_name}:{field_sig}')
    pos += 8

# Parse methods
num_methods = struct.unpack('>H', data[pos:pos+2])[0]
pos += 2
clinit_code = None
for mi in range(num_methods):
    meth_name_idx = struct.unpack('>H', data[pos:pos+2])[0]
    meth_sig_idx = struct.unpack('>H', data[pos+2:pos+4])[0]
    meth_name = entries.get(meth_name_idx, ('?',''))[1] if meth_name_idx in entries else '?'
    meth_sig = entries.get(meth_sig_idx, ('?',''))[1] if meth_sig_idx in entries else '?'
    meth_access = struct.unpack('>H', data[pos+4:pos+6])[0]
    num_attrs = struct.unpack('>H', data[pos+6:pos+8])[0]
    ap = pos + 8
    for _ in range(num_attrs):
        attr_name_idx = struct.unpack('>H', data[ap:ap+2])[0]
        attr_len = struct.unpack('>I', data[ap+2:ap+6])[0]
        attr_name = entries.get(attr_name_idx, ('?',''))[1] if attr_name_idx in entries else '?'
        if attr_name == 'Code' and meth_name == '<clinit>' and meth_sig == '()V':
            code_len = struct.unpack('>I', data[ap+8:ap+12])[0]
            clinit_code = (ap + 12, code_len)
            break
        ap += 8 + attr_len
    pos += 8

print()
if clinit_code:
    code_start, code_len = clinit_code
    print(f'<clinit> code at byte offset {code_start}, length {code_len}')
    print('Instructions:')
    j = 0
    while j < code_len:
        op = data[code_start + j]
        if op == 0xb2:  # getstatic
            idx = struct.unpack('>H', data[code_start+j+1:code_start+j+3])[0]
            ent = entries.get(idx, ('?',))
            if ent[0] == 'FIELDREF':
                cls_ent = entries.get(ent[1], ('?', '?'))
                nt_ent = entries.get(ent[2], ('?', '?', '?'))
                nt_name_ent = entries.get(nt_ent[1], ('?', ''))
                cls_name = cls_ent[1] if isinstance(cls_ent, tuple) else '?'
                nt_name = nt_name_ent[1] if isinstance(nt_name_ent, tuple) else '?'
                print(f'  [{j:3d}] getstatic #{idx} = {cls_name}.{nt_name}')
            else:
                print(f'  [{j:3d}] getstatic #{idx} = {ent}')
            j += 3
        elif op == 0xb3:  # putstatic
            idx = struct.unpack('>H', data[code_start+j+1:code_start+j+3])[0]
            ent = entries.get(idx, ('?',))
            if ent[0] == 'FIELDREF':
                cls_ent = entries.get(ent[1], ('?', '?'))
                nt_ent = entries.get(ent[2], ('?', '?', '?'))
                nt_name_ent = entries.get(nt_ent[1], ('?', ''))
                cls_name = cls_ent[1] if isinstance(cls_ent, tuple) else '?'
                nt_name = nt_name_ent[1] if isinstance(nt_name_ent, tuple) else '?'
                print(f'  [{j:3d}] putstatic #{idx} = {cls_name}.{nt_name}')
            else:
                print(f'  [{j:3d}] putstatic #{idx} = {ent}')
            j += 3
        elif op == 0x28:  # bipush
            print(f'  [{j:3d}] bipush {data[code_start+j+1]}')
            j += 2
        elif op in (0x15, 0x16, 0x17, 0x18, 0x19):  # iload_0-4
            print(f'  [{j:3d}] iload_{op-0x15}')
            j += 1
        elif op == 0x02:  # iconst_0
            print(f'  [{j:3d}] iconst_0')
            j += 1
        elif op == 0x03:  # iconst_1
            print(f'  [{j:3d}] iconst_1')
            j += 1
        elif op == 0x59:  # ireturn
            print(f'  [{j:3d}] ireturn')
            j += 1
        elif op == 0x04:  # aconst_null
            print(f'  [{j:3d}] aconst_null')
            j += 1
        elif op == 0xb1:  # invokevirtual
            idx = struct.unpack('>H', data[code_start+j+1:code_start+j+3])[0]
            ent = entries.get(idx, ('?',))
            if ent[0] == 'METHODREF':
                cls_ent = entries.get(ent[1], ('?', '?'))
                nt_ent = entries.get(ent[2], ('?', '?', '?'))
                nt_name_ent = entries.get(nt_ent[1], ('?', ''))
                cls_name = cls_ent[1] if isinstance(cls_ent, tuple) else '?'
                nt_name = nt_name_ent[1] if isinstance(nt_name_ent, tuple) else '?'
                print(f'  [{j:3d}] invokevirtual #{idx} = {cls_name}.{nt_name}')
            else:
                print(f'  [{j:3d}] invokevirtual #{idx} = {ent}')
            j += 3
        elif op == 0xb6:  # getfield
            idx = struct.unpack('>H', data[code_start+j+1:code_start+j+3])[0]
            ent = entries.get(idx, ('?',))
            if ent[0] == 'FIELDREF':
                cls_ent = entries.get(ent[1], ('?', '?'))
                nt_ent = entries.get(ent[2], ('?', '?', '?'))
                nt_name_ent = entries.get(nt_ent[1], ('?', ''))
                cls_name = cls_ent[1] if isinstance(cls_ent, tuple) else '?'
                nt_name = nt_name_ent[1] if isinstance(nt_name_ent, tuple) else '?'
                print(f'  [{j:3d}] getfield #{idx} = {cls_name}.{nt_name}')
            else:
                print(f'  [{j:3d}] getfield #{idx} = {ent}')
            j += 3
        elif op == 0xb7:  # invokenpecial
            idx = struct.unpack('>H', data[code_start+j+1:code_start+j+3])[0]
            ent = entries.get(idx, ('?',))
            if ent[0] == 'METHODREF':
                cls_ent = entries.get(ent[1], ('?', '?'))
                nt_ent = entries.get(ent[2], ('?', '?', '?'))
                nt_name_ent = entries.get(nt_ent[1], ('?', ''))
                cls_name = cls_ent[1] if isinstance(cls_ent, tuple) else '?'
                nt_name = nt_name_ent[1] if isinstance(nt_name_ent, tuple) else '?'
                print(f'  [{j:3d}] invokenpecial #{idx} = {cls_name}.{nt_name}')
            else:
                print(f'  [{j:3d}] invokenpecial #{idx} = {ent}')
            j += 3
        elif op == 0x5a:  # istore_0
            print(f'  [{j:3d}] istore_0')
            j += 1
        else:
            print(f'  [{j:3d}] opcode=0x{op:02x}')
            j += 1
