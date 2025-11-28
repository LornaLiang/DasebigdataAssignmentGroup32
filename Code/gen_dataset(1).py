import argparse
import random
import os


# =========================================
# 不同分布的行生成函数
# =========================================

def generate_uniform_line():
    """均匀分布：key 在 [1, 1e6] 随机"""
    key = random.randint(1, 1_000_000)
    return f"{key} 1\n"


def generate_skew60_line():
    """
    中度倾斜：60% 都来自同一个 key，例如 key=1
    40% 在 [2,1000] 内随机
    """
    if random.random() < 0.6:   # 60% 热点
        key = 1                 # 单一热点
    else:
        key = random.randint(2, 1000)
    return f"{key} 1\n"


def generate_extreme90_line():
    """
    极端倾斜：90% 都来自同一个 key，例如 key=1
    10% 在 [2,1000] 内随机
    """
    if random.random() < 0.9:   # 90% 热点
        key = 1
    else:
        key = random.randint(2, 1000)
    return f"{key} 1\n"


# =========================================
# 文件生成主函数
# =========================================

def generate_file(gen_func, target_mb, output_path):
    target_bytes = target_mb * 1024 * 1024
    written = 0
    buffer = []
    flush_interval = 10000  # 每1万行写一次文件，提高性能

    with open(output_path, "w") as f:
        while written < target_bytes:
            line = gen_func()
            buffer.append(line)
            written += len(line)

            if len(buffer) >= flush_interval:
                f.writelines(buffer)
                buffer = []

        # flush 剩余内容
        if buffer:
            f.writelines(buffer)

    actual = os.path.getsize(output_path)
    print(f"✔ 生成完成：{output_path}")
    print(f"  目标大小：{target_mb} MB")
    print(f"  实际大小：{actual/1024/1024:.2f} MB")


# =========================================
# 主运行入口
# =========================================

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--type", choices=["uniform", "skew60", "extreme90"], required=True)
    parser.add_argument("--size", type=int, required=True)
    parser.add_argument("--output", type=str, required=True)

    args = parser.parse_args()

    if args.type == "uniform":
        gen = generate_uniform_line
    elif args.type == "skew60":
        gen = generate_skew60_line
    elif args.type == "extreme90":
        gen = generate_extreme90_line

    print(f"开始生成：{args.output}")
    generate_file(gen, args.size, args.output)


if __name__ == "__main__":
    main()
