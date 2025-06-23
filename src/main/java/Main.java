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
    private static final int WIDTH = 80;
    private static final int HEIGHT = 40;
    private static final int CELL_SIZE = 20;

    private static class State {
        SDL_Window window;
        SDL_Renderer renderer;
        int[][] grid;
        boolean running;
        boolean paused;

        public static final Mouse mouse = new Mouse();

        State() {
            grid = new int[HEIGHT][WIDTH];
            running = true;
            paused = false;
        }

        static class Mouse {
            int x, y;
        }
    }

    private static final State state = new State();

    private static int getNeighbors(int y, int x) {
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
                        neighbors += state.grid[ny][nx];
                    }
                }
            }
        }
        return neighbors;
    }

    private static void renderGrid() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                SDL_Rect cell = new SDL_Rect();
                cell.x = x * CELL_SIZE;
                cell.y = y * CELL_SIZE;
                cell.w = CELL_SIZE;
                cell.h = CELL_SIZE;

                if (state.grid[y][x] == 1)
                    SDL_SetRenderDrawColor(state.renderer, (byte) 0, (byte) 150, (byte) 0, (byte) 255);
                else if ((x + y) % 2 == 1)
                    SDL_SetRenderDrawColor(state.renderer, (byte) 20, (byte) 20, (byte) 20, (byte) 255);
                else
                    SDL_SetRenderDrawColor(state.renderer, (byte) 0, (byte) 0, (byte) 0, (byte) 255);

                SDL_RenderFillRect(state.renderer, cell);
            }
        }
    }

    private static void updateGrid() {
        int[][] newGrid = new int[HEIGHT][WIDTH];

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int neighbors = getNeighbors(y, x);

                // Apply Conway's Game of Life rules
                // 1. Any live cell with 2 or 3 live neighbors survives
                // 2. Any dead cell with exactly 3 live neighbors becomes alive
                // 3. All other cells die or stay dead
                newGrid[y][x] = (neighbors == 3 || (state.grid[y][x] == 1 && neighbors == 2)) ? 1 : 0;
            }
        }

        // Update mouse pos
        IntByReference mx = new IntByReference();
        IntByReference my = new IntByReference();
        SDL_GetMouseState(mx, my);
        state.mouse.y = my.getValue() / CELL_SIZE;
        state.mouse.x = mx.getValue() / CELL_SIZE;

        // Paste updated grid into viewable grid
        if (state.paused) return;
        for (int y = 0; y < HEIGHT; y++) {
            System.arraycopy(newGrid[y], 0, state.grid[y], 0, WIDTH);
        }
    }

    public static void main(String[] args) {
        // Initialize with a simple pattern (glider)
        state.grid[1][2] = 1;
        state.grid[2][3] = 1;
        state.grid[3][1] = 1;
        state.grid[3][2] = 1;
        state.grid[3][3] = 1;

        state.window = SDL_CreateWindow("Game of Life", SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED, WIDTH * CELL_SIZE, HEIGHT * CELL_SIZE, SDL_WINDOW_SHOWN);
        state.renderer = SDL_CreateRenderer(state.window, -1, SDL_RENDERER_ACCELERATED);

        SDL_Event ev = new SDL_Event();
        while (state.running) {
            while (SDL_PollEvent(ev) != 0) {
                if (ev.type == SDL_QUIT) state.running = false;
                if (ev.type == SDL_MOUSEBUTTONDOWN) state.grid[state.mouse.y][state.mouse.x] = 1;
                if (ev.type == SDL_KEYDOWN && ev.key.keysym.sym == SDLK_SPACE) {state.paused = !state.paused; System.out.println("PAUSED"); };
            }

            renderGrid();
            SDL_RenderPresent(state.renderer);
            SDL_Delay(200);
            updateGrid();

            System.out.printf("MOUSE POS: %d / %d \n", state.mouse.x, state.mouse.y);
        }

        // Clean up
        SDL_DestroyRenderer(state.renderer);
        SDL_DestroyWindow(state.window);
        SDL_Quit();
    }
}
