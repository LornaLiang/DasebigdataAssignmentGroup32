import random

def gen_avg_dataset(filename, total_bytes, num_groups=1):
    """
    生成一个可用于“求平均值”实验的数据集。
    文件格式：<group_id> <value>
    group_id：分组键（可全部为1）
    value：随机整数，范围 0-1000
    total_bytes：目标文件大小（以字节为单位）
    """
    written = 0
    with open(filename, "w") as f:
        while written < total_bytes:
            group_id = random.randint(1, num_groups)   # 如果你只想一个组，就设 num_groups=1
            value = random.randint(0, 1000)
            line = f"{group_id} {value}\n"
            f.write(line)
            written += len(line)

    print(f"Generated {filename}, size ≈ {total_bytes/1024/1024:.2f} MB")


if __name__ == "__main__":
    MB = 1024 * 1024

    # 生成 50MB 的平均值数据集（单组）
    gen_avg_dataset("avg_50MB.txt", 50 * MB, num_groups=1)
