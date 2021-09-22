package badgamesinc.hypnotic.ui.clickgui2.frame;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;

import badgamesinc.hypnotic.config.SaveLoad;
import badgamesinc.hypnotic.module.Category;
import badgamesinc.hypnotic.module.Mod;
import badgamesinc.hypnotic.module.ModuleManager;
import badgamesinc.hypnotic.module.hud.HudManager;
import badgamesinc.hypnotic.module.hud.HudModule;
import badgamesinc.hypnotic.module.render.ClickGUIModule;
import badgamesinc.hypnotic.ui.clickgui2.frame.button.Button;
import badgamesinc.hypnotic.ui.clickgui2.frame.button.settings.ColorBox;
import badgamesinc.hypnotic.ui.clickgui2.frame.button.settings.Component;
import badgamesinc.hypnotic.utils.ColorUtils;
import badgamesinc.hypnotic.utils.font.FontManager;
import badgamesinc.hypnotic.utils.render.RenderUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public class Frame {

	public Category category;
	public String name;
	private int x, y, width, height, dragX, dragY;
	private boolean extended, dragging;
	public ArrayList<Button> buttons;
	public Color color = Color.decode(ColorUtils.pingle);
	
	public Frame(int x, int y, int width, int height, Category category) {
		this.category = category;
		this.name = category.name;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.extended = true;
		this.buttons = new ArrayList<Button>();
		
		int offset = height;
		for (Mod mod : ModuleManager.INSTANCE.getModulesInCategory(category)) {
			buttons.add(new Button(mod, this.x, this.y, offset, this));
			offset+=height;
		}
	}
	
	public Frame(int x, int y, int width, int height, String name) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.extended = true;
		this.buttons = new ArrayList<Button>();
		int offset = height;
		for (HudModule mod : HudManager.INSTANCE.hudModules) {
			buttons.add(new Button(mod, this.x, this.y, offset, this));
			offset+=height;
		}
	}
	
	float animTicks = 0;
	int length = 0;
	public void render(MatrixStack matrices, int mouseX, int mouseY) {
//		RenderUtils.startScissor(this.x, this.y, this.width, (int)animTicks);
		this.color = category != null && !ModuleManager.INSTANCE.getModule(ClickGUIModule.class).customColor.is("Custom") ? category.color : ModuleManager.INSTANCE.getModule(ClickGUIModule.class).color.getColor();
		Screen.fill(matrices, x, y, x + width, y + height, color.getRGB());
		Screen.fill(matrices, x + 1, y + 1, x + width - 1, y + height - (this.extended ? 0 : 0), new Color(25, 25, 25).getRGB());
		FontManager.roboto.drawWithShadow(matrices, name, x + (height / 3), y + (height / 6), -1);
		FontManager.roboto.drawWithShadow(matrices, extended ? "-" : "+", x + width - (height / 1.5f), y + (height / 6), -1);
		if (this.extended) {
			buttons.sort(Comparator.comparingInt(b -> (int)FontManager.roboto.getWidth(((Button)b).mod.getName())).reversed());
			for (Button button : buttons) {
				button.setWidth(this.width);
				button.render(matrices, mouseX, mouseY);
				int count2 = 0;
				if (!button.isExtended()) length = buttons.size() * height;
				if (this.animTicks > length + height + 1) animTicks--;
				if (this.extended) if (animTicks < length + height + 1) {
					animTicks++;
				}
				for (Component component : button.components) {
					if (button.isExtended()) length = (buttons.size() * height) + count2;
					if (button.isExtended() || button.animation > 0) {
						if (component.setting.isVisible()) {
							component.render(matrices, mouseX, mouseY, count2);
							Screen.fill(matrices, x, button.getY() + height, x + 1, button.getY() + height*2 + count2, color.getRGB());
							Screen.fill(matrices, x + width, button.getY() + height, x + width - 1, button.getY() + height*2 + count2, color.getRGB());
							count2+=(component instanceof ColorBox ? height * 7.5f : height);
							
							if (!button.isExtended() && button.animation < count2 && this.extended) button.animation++;
							if (this.extended && !button.isExtended() && button.animation > 0) button.animation--;
							if (animTicks < count2 + length + height + 1 && button.isExtended()) {
								animTicks++;
							}
							if (animTicks > length + height + 1 && !button.isExtended()) animTicks--;
						}
						
					}
				}
				if (buttons.indexOf(button) == buttons.size() - 1) {
					if (!button.isExtended()) {
						Screen.fill(matrices, button.getX(), button.getY() + this.height, button.getX() + button.getWidth(), button.getY() + this.height + 1, color.getRGB());
					} else {
						for (Component c : button.components)
							if (button.components.indexOf(c) == button.components.size() - 1)
								Screen.fill(matrices, button.getX(), button.getY() + this.height*2 + c.offset, button.getX() + button.getWidth(), button.getY() + this.height*2 + c.offset + 1, color.getRGB());
					}
					
					
				}
				
			}
			
		} if (!this.extended) {

			length = height;
			if (animTicks > this.height + 1) {
				animTicks-=5;
			} else {
				RenderUtils.drawBorderRect(matrices, this.x, this.y, this.x + this.width, this.y + this.height + 1, color.getRGB(), 1);
			}
			if (animTicks < this.height + 1) {
				animTicks = this.height + 1;
			}
		}
		if (this.extended) {
//			if (animTicks <= 201) animTicks+=5;
		} else {
			
		}
//		System.out.println(animTicks + this.name);
		RenderUtils.endScissor();
//		Screen.fill(matrices, this.x, this.y, this.x + this.width, this.y + 100, ColorUtils.transparent(-1, 50));
		
	}
	
	public void mouseClicked(double mouseX, double mouseY, int button) {
		if (hovered(mouseX, mouseY)) {
			if (button == 0) {
				setDragging(true);
				setDragX((int) (mouseX - getX()));
				setDragY((int) (mouseY - getY()));
			}
			if (button == 1) {
				this.extended = !this.extended;
			}
		}
		
		for (Button modButton : buttons) {
			if (this.extended) modButton.mouseClicked(mouseX, mouseY, button);
		}
	}
	
	public boolean hovered(double mouseX, double mouseY) {
		return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
	}
	
	public void updateButtons() {
		int offset = (int) (height * 1);
		for (Button button : buttons) {
			button.setY(this.y + offset);
			button.setX(this.getX());
			offset+=height;
			if (button.isExtended()) {
				for (Component component : button.components) {
					if (component.setting.isVisible()) offset+=(component instanceof ColorBox ? height * 7.5f : height); 
				}
			}
			
		}
	}
	
	public void updatePosition(int mouseX, int mouseY) {
		if (isDragging()) {
			setX(mouseX - dragX);
			setY(mouseY - dragY);
		}
	}
	
	public void mouseReleased(int button) {
		if (button == 0) setDragging(false);
		for (Button funnyButton : buttons) {
			funnyButton.mouseReleased(button);
		}
	}
	
	public boolean isDragging() {
		return dragging;
	}
	
	public void setDragging(boolean dragging) {
		if (dragging)
		SaveLoad.INSTANCE.save();
		this.dragging = dragging;
	}
	
	public int getDragX() {
		return dragX;
	}
	
	public int getDragY() {
		return dragY;
	}
	
	public void setDragX(int dragX) {
		this.dragX = dragX;
	}
	
	public void setDragY(int dragY) {
		this.dragY = dragY;
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
