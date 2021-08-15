package badgamesinc.hypnotic.ui.clickgui2.frame;

import java.awt.Color;
import java.util.ArrayList;

import badgamesinc.hypnotic.module.Category;
import badgamesinc.hypnotic.module.Mod;
import badgamesinc.hypnotic.module.ModuleManager;
import badgamesinc.hypnotic.ui.clickgui2.frame.button.Button;
import badgamesinc.hypnotic.utils.ColorUtils;
import badgamesinc.hypnotic.module.render.ClickGUIModule;
import net.minecraft.client.MinecraftClient;
import badgamesinc.hypnotic.utils.font.FontManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public class Frame {

	public Category category;
	private int x, y, width, height;
	private boolean extended;
	private ArrayList<Button> buttons;
	
	public Frame(int x, int y, int width, int height, Category category) {
		this.category = category;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.extended = true;
		this.buttons = new ArrayList<Button>();
		
		int offset = height;
		for (Mod mod : ModuleManager.INSTANCE.getModulesInCategory(category)) {
			buttons.add(new Button(mod, this.x, this.y + offset, this));
			offset+=height;
		}
	}
	
	public void render(MatrixStack matrices, int mouseX, int mouseY) {
		int color = ModuleManager.INSTANCE.getModule(ClickGUIModule.class).customColor.isEnabled() ? new Color((int)ModuleManager.INSTANCE.getModule(ClickGUIModule.class).red.getValue(), (int)ModuleManager.INSTANCE.getModule(ClickGUIModule.class).green.getValue(), (int)ModuleManager.INSTANCE.getModule(ClickGUIModule.class).blue.getValue()).getRGB() : ColorUtils.getClientColorInt();
		Screen.fill(matrices, x, y, x + width, y + height, color);
		Screen.drawStringWithShadow(matrices, MinecraftClient.getInstance().textRenderer, category.name, x + 4, y + 4, -1);
		Screen.drawStringWithShadow(matrices, MinecraftClient.getInstance().textRenderer, extended ? "-" : "+", x + width - 10, y + 4, -1);
		
		Screen.fill(matrices, x, y, x + width, y + height, category.color.getRGB());
		Screen.fill(matrices, x + 1, y + 1, x + width - 1, y + height - (this.extended ? 0 : 1), new Color(25, 25, 25).getRGB());
		FontManager.roboto.drawWithShadow(matrices, category.name, x + 4, y, -1, true);
		FontManager.roboto.drawWithShadow(matrices, extended ? "-" : "+", x + width - 10, y, -1, true);
		for (Button button : buttons) {
			if (this.extended) {
				button.render(matrices, mouseX, mouseY);
				if (buttons.indexOf(button) == buttons.size() - 1) {
					if (!button.isExtended()) Screen.fill(matrices, button.getX(), button.getY() + this.height, button.getX() + button.getWidth(), button.getY() + this.height + 1, category.color.getRGB());
					else Screen.fill(matrices, button.getX(), button.getY() + this.height + button.components.size() * height, button.getX() + button.getWidth(), button.getY() + this.height + button.components.size() * height + 1, category.color.getRGB());
				}
			}
		}
	}
	
	public void mouseClicked(double mouseX, double mouseY, int button) {
		if (hovered(mouseX, mouseY)) {
			if (button == 1) {
				this.extended = !this.extended;
			}
		}
		
		for (Button modButton : buttons) {
			modButton.mouseClicked(mouseX, mouseY, button);
		}
	}
	
	public boolean hovered(double mouseX, double mouseY) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}
	
	public void updateButtons() {
		int offset = (int) (height * 2.45);
		for (Button button : buttons) {
			button.setY(offset);
			
			offset+=height + (button.isExtended() ? button.mod.settings.size() * height : 0);
			
		}
	}
	
	public void mouseReleased(int button) {
		for (Button funnyButton : buttons) {
			funnyButton.mouseReleased(button);
		}
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public boolean isExtended() {
		return extended;
	}

	public void setExtended(boolean extended) {
		this.extended = extended;
	}
}
