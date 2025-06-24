import io.github.libsdl4j.api.event.*;
import io.github.libsdl4j.api.render.*;
import io.github.libsdl4j.api.video.*;
import io.github.libsdl4j.api.rect.*;
import com.sun.jna.ptr.IntByReference;

import static io.github.libsdl4j.api.Sdl.*;
import static io.github.libsdl4j.api.event.SDL_EventType.*;
import static io.github.libsdl4j.api.event.SdlEvents.*;
import static io.github.libsdl4j.api.keycode.SDL_Keycode.*;
import static io.github.libsdl4j.api.mouse.SdlMouse.*;
import static io.github.libsdl4j.api.render.SDL_RendererFlags.*;
import static io.github.libsdl4j.api.render.SdlRender.*;
import static io.github.libsdl4j.api.timer.SdlTimer.*;
import static io.github.libsdl4j.api.video.SDL_WindowFlags.*;
import static io.github.libsdl4j.api.video.SdlVideo.*;
import static io.github.libsdl4j.api.video.SdlVideoConst.*;

public class Main {
    static final int WIDTH = 80;
    static final int HEIGHT = 40;
    static final int CELL_SIZE = 20;

    static class State {
        static SDL_Window window;
        static SDL_Renderer renderer;
        static int[][] grid;
        static boolean running;
        static boolean paused;

        static class Mouse {
            static int x, y;
        }
    }

    static int getNeighbors(int y, int x) {
        int neighbors = 0;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx != 0 || dy != 0) {
                    int ny = y + dy;
                    int nx = x + dx;
                    if (ny >= 0
                            && ny < HEIGHT
                            && nx >= 0
                            && nx < WIDTH) {
                        neighbors += State.grid[ny][nx];
                    }
                }
            }
        }
        return neighbors;
    }

    static void renderGrid() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                SDL_Rect cell = new SDL_Rect();
                cell.x = x * CELL_SIZE;
                cell.y = y * CELL_SIZE;
                cell.w = CELL_SIZE;
                cell.h = CELL_SIZE;

                if (State.grid[y][x] == 1)
                    SDL_SetRenderDrawColor(State.renderer, (byte) 0, (byte) 150, (byte) 0, (byte) 255);
                else if ((x + y) % 2 == 1)
                    SDL_SetRenderDrawColor(State.renderer, (byte) 20, (byte) 20, (byte) 20, (byte) 255);
                else
                    SDL_SetRenderDrawColor(State.renderer, (byte) 0, (byte) 0, (byte) 0, (byte) 255);

                SDL_RenderFillRect(State.renderer, cell);
            }
        }
    }

    static void updateGrid() {
        int[][] newGrid = new int[HEIGHT][WIDTH];

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int neighbors = getNeighbors(y, x);

                // Apply Conway's Game of Life rules
                // 1. Any live cell with 2 or 3 live neighbors survives
                // 2. Any dead cell with exactly 3 live neighbors becomes alive
                // 3. All other cells die or stay dead
                newGrid[y][x] = (neighbors == 3 || (State.grid[y][x] == 1 && neighbors == 2)) ? 1 : 0;
            }
        }

        // Update mouse pos
        IntByReference mx = new IntByReference();
        IntByReference my = new IntByReference();
        SDL_GetMouseState(mx, my);
        State.Mouse.y = my.getValue() / CELL_SIZE;
        State.Mouse.x = mx.getValue() / CELL_SIZE;

        // Paste updated grid into viewable grid
        if (State.paused) return;
        for (int y = 0; y < HEIGHT; y++) {
            System.arraycopy(newGrid[y], 0, State.grid[y], 0, WIDTH);
        }
    }

    static void spawn_ship() {
        int x = State.Mouse.x;
        int y = State.Mouse.y;

        State.grid[y][x] =
        State.grid[y + 1][x] =
        State.grid[y + 2][x] =
        State.grid[y + 3][x] =
        State.grid[y + 1][x + 1] =
        State.grid[y + 2][x + 1] =
        State.grid[y + 3][x + 1] =
        State.grid[y + 1][x + 2] =
        State.grid[y + 2][x + 2] =
        State.grid[y + 3][x + 2] =
        State.grid[y + 1][x + 3] =
        State.grid[y + 2][x + 3] =
        State.grid[y + 3][x + 3] = 1;
    }

    static void spawn_glider() {
        int x = State.Mouse.x;
        int y = State.Mouse.y;

        State.grid[y][x] =
        State.grid[y + 1][x + 1] =
        State.grid[y + 2][x - 1] =
        State.grid[y + 2][x] =
        State.grid[y + 2][x + 1] = 1;
    }

    static void init() {
        State.grid = new int[HEIGHT][WIDTH];
        State.window = SDL_CreateWindow("Game of Life", SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED, WIDTH * CELL_SIZE, HEIGHT * CELL_SIZE, SDL_WINDOW_SHOWN);
        State.renderer = SDL_CreateRenderer(State.window, -1, SDL_RENDERER_ACCELERATED);
        State.running = true;
        State.paused = true;
    }

    static void deinit() {
        // Clean up
        SDL_DestroyRenderer(State.renderer);
        SDL_DestroyWindow(State.window);
        SDL_Quit();
    }

    public static void main(String[] args) {
        init();
        while (State.running) {
            SDL_Event ev = new SDL_Event();
            while (SDL_PollEvent(ev) != 0) {
                switch (ev.type) {
                case SDL_QUIT:
                    State.running = false;
                    break;
                case SDL_MOUSEBUTTONDOWN:
                    if (State.grid[State.Mouse.y][State.Mouse.x] == 1) State.grid[State.Mouse.y][State.Mouse.x] = 0;
                    else State.grid[State.Mouse.y][State.Mouse.x] = 1;
                    break;
                case SDL_KEYDOWN:
                    switch (ev.key.keysym.sym) {
                    case SDLK_SPACE:
                        State.paused = !State.paused;
                        System.out.println("PAUSED");
                        break;
                    case SDLK_RETURN:
                        State.paused = !State.paused;
                        updateGrid();
                        System.out.print("UPDATED ONCE");
                        State.paused = !State.paused;
                        break;
                    case SDLK_BACKSPACE:
                        State.grid = new int[HEIGHT][WIDTH];
                        System.out.print("DELETED");
                        break;
                    case SDLK_S:
                        spawn_ship();
                        break;
                    case SDLK_G:
                        spawn_glider();
                        break;
                    }
                }
            }

            renderGrid();
            SDL_RenderPresent(State.renderer);
            SDL_Delay(200);
            updateGrid();

            System.out.printf("MOUSE POS: %d / %d \n", State.Mouse.x, State.Mouse.y);
        }
        deinit();
    }
}
