package javax0.flupoi;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.formula.eval.NotImplementedException;
import org.apache.poi.ss.usermodel.Cell;

class RangeIterator implements Iterable<Collection<Cell>>,
		Iterator<Collection<Cell>> {

	Processor processor;

	protected RangeIterator(final Processor processor) {
		this.processor = processor;
	}

	@Override
	public Iterator<Collection<Cell>> iterator() {
		switch (processor.getRange().getDirection()) {
		case RIGHT:
			setPosition(processor.getRange().getStart().getXValue());
			break;
		case LEFT:
			setPosition(processor.getRange().getEnd().getXValue());
			break;
		case DOWN:
			setPosition(processor.getRange().getStart().getYValue());
			break;
		case UP:
			setPosition(processor.getRange().getEnd().getYValue());
			break;
		}
		return this;
	}

	private void stepPosition() {
		switch (processor.getRange().getDirection()) {
		case RIGHT:
			position++;
			break;
		case LEFT:
			position--;
			break;
		case UP:
			position--;
			break;
		case DOWN:
			position++;
			break;
		}

	}

	private Collection<Cell> cells = null;

	private boolean isForwardRange() {
		RangeDirection direction = processor.getRange().getDirection();
		return direction == RangeDirection.RIGHT
				|| direction == RangeDirection.DOWN;
	}

	private boolean isVerticalRange() {
		RangeDirection direction = processor.getRange().getDirection();
		return direction == RangeDirection.RIGHT
				|| direction == RangeDirection.LEFT;
	}

	private void collectCells() {
		cells = new LinkedList<Cell>();
		int start = 0;
		int end = 0;
		if (isVerticalRange()) {
			start = processor.getRange().getStart().getYValue();
			end = processor.getRange().getEnd().getYValue();
		} else {
			start = processor.getRange().getStart().getXValue();
			end = processor.getRange().getEnd().getXValue();
		}
		for (int i = start; (isForwardRange() ? i <= end : i >= end); i = isForwardRange() ? i + 1
				: i - 1) {
			try {
				cells.add(processor.getProcessState()
						.getSheet(processor.getSheetName())
						.getRow(isVerticalRange() ? i : position)
						.getCell(isVerticalRange() ? position : i));
			} catch (NullPointerException | InvalidFormatException
					| IOException e) {
				cells = null;
				return;
			}
		}

	}

	private boolean stopNow;

	@Override
	public boolean hasNext() {
		if (cells == null) {
			collectCells();
			stopNow = cells == null ? true
					: (processor.getCondition() == null ? (isVerticalRange() ? position > processor
							.getRange().getEnd().getXValue()
							|| position < processor.getRange().getStart()
									.getXValue()
							: position > processor.getRange().getEnd().getYValue()
									|| position < processor.getRange()
											.getStart().getYValue())
							: processor.getCondition().match(cells));
		}
		return !stopNow;
	}

	@Override
	public Collection<Cell> next() {
		if (cells == null) {
			collectCells();
		}
		final Collection<Cell> newCells;
		if (hasNext()) {

			newCells = cells;
			cells = null;
			stepPosition();
		} else {
			newCells = null;
		}
		return newCells;
	}

	@Override
	public void remove() {
		throw new NotImplementedException(
				"remove() is not implemented in RangeIterator");

	}

	public int getPosition() {
		return position;
	}

	public void setPosition(final int position) {
		this.position = position;
	}

	private int position;
}
