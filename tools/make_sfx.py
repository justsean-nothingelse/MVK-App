"""Generates the randomizer sound effects into app/src/main/res/raw/.

Run:  python3 tools/make_sfx.py
Edit the numbers below to change how the sounds feel, then rebuild the app.
"""
import math
import os
import random
import struct
import wave

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")


def render(ms, fn):
    n = int(SR * ms / 1000)
    return [fn(i / SR) for i in range(n)]


def normalize(samples, peak):
    m = max(abs(s) for s in samples) or 1.0
    return [s / m * peak for s in samples]


def fade_out(samples, ms):
    n = min(len(samples), int(SR * ms / 1000))
    for k in range(n):
        samples[-1 - k] *= k / n
    return samples


def save(name, samples):
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, name)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 32767)) for s in samples))
    print(f"{name}: {len(samples) / SR * 1000:.0f} ms, peak {max(abs(s) for s in samples):.2f}")


rng = random.Random(7)

# Spin "clack": a short woody click, played for every shuffling frame (pitch varies in the app).
noise = [rng.uniform(-1, 1) for _ in range(SR)]
spin = render(22, lambda t: (
    math.sin(2 * math.pi * 1350 * t) * math.exp(-t * 190)
    + 0.6 * math.sin(2 * math.pi * 2700 * t) * math.exp(-t * 260)
    + 0.5 * noise[int(t * SR)] * math.exp(-t * 900)
))
save("sfx_spin.wav", fade_out(normalize(spin, 0.65), 4))

# Lock-in "tick": brighter, with a small low thump, when a character locks in place.
tick = render(45, lambda t: (
    min(1, t * 1000) * (
        math.sin(2 * math.pi * 2100 * t) * math.exp(-t * 95)
        + 0.35 * math.sin(2 * math.pi * 3150 * t) * math.exp(-t * 130)
        + 0.5 * math.sin(2 * math.pi * 170 * t) * math.exp(-t * 110)
    )
))
save("sfx_tick.wav", fade_out(normalize(tick, 0.85), 6))


# Win chime: quick rising G-B-D-G arpeggio that rings out like a bell.
def note(freq, start, decay):
    def f(t):
        u = t - start
        if u < 0:
            return 0.0
        env = min(1, u * 250) * math.exp(-u * decay)
        return env * (
            math.sin(2 * math.pi * freq * u)
            + 0.25 * math.sin(2 * math.pi * freq * 2 * u)
            + 0.08 * math.sin(2 * math.pi * freq * 3 * u)
        )
    return f


notes = [note(784, 0.000, 9), note(988, 0.055, 9), note(1175, 0.110, 8), note(1568, 0.165, 5)]
win = render(720, lambda t: sum(n(t) for n in notes))
save("sfx_win.wav", fade_out(normalize(win, 0.85), 90))
