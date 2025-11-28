import random

def gen_uniform(filename, total_bytes):
    """生成均匀分布 key-count 数据集"""
    with open(filename, "w") as f:
        written = 0
        while written < total_bytes:
            key = random.randint(1, 10**6)
            line = f"{key} 1\n"
            f.write(line)
            written += len(line)
    print(f"Generated {filename}, size ≈ {total_bytes/1024/1024} MB")


def gen_skew60(filename, total_bytes):
    """生成60%倾斜 key-count 数据集"""
    hot_key = 777777  # 热点key
    skew_ratio = 0.6  # 60% 倾斜

    with open(filename, "w") as f:
        written = 0
        while written < total_bytes:
            if random.random() < skew_ratio:
                key = hot_key
            else:
                key = random.randint(1, 10**6)
            line = f"{key} 1\n"
            f.write(line)
            written += len(line)
    print(f"Generated {filename}, size ≈ {total_bytes/1024/1024} MB")


if __name__ == "__main__":
    MB = 1024 * 1024

    # === 均匀分布 ===
    gen_uniform("uniform_25MB.txt", 25 * MB)
    gen_uniform("uniform_50MB.txt", 50 * MB)

    # === 60% 倾斜 ===
    gen_skew60("skew60_25MB.txt", 25 * MB)
    gen_skew60("skew60_50MB.txt", 50 * MB)
