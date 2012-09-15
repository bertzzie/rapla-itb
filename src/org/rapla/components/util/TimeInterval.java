package org.rapla.components.util;

import java.util.Date;

public final class TimeInterval 
{
	Date start;
	Date end;

	public TimeInterval(Date start, Date end) {
		this.start = start;
		this.end = end;
	}
	
	public Date getStart() {
		return start;
	}
	public void setStart(Date start) {
		this.start = start;
	}
	public Date getEnd() {
		return end;
	}
	public void setEnd(Date end) {
		this.end = end;
	}
	
	public String toString()
	{
		return start + " - " + end;
	}
	
	public boolean equals(Object obj)
	{
		if ( !(obj instanceof TimeInterval) || obj == null)
		{
			return false;
		}
		TimeInterval other = (TimeInterval) obj;
		Date start2 = other.getStart();
		Date end2 = other.getEnd();
		
		if ( start == null  )
		{
			if (start != start2)
			{
				return false;
			}
		}
		else
		{
			if ( start2 == null || !start.equals(start2))
			{
				return false;
			}
		}
		
		if ( end == null  )
		{
			if (end != end2)
			{
				return false;
			}
		}
		else
		{
			if ( end2 == null || !end.equals(end2))
			{
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public int hashCode() 
	{
		int hashCode;
		if ( start!=null )
		{
			hashCode = start.hashCode(); 
			if ( end!=null )
			{
				hashCode *= end.hashCode();
			}
		}
		else if ( end!=null )
		{
			hashCode = end.hashCode(); 
		}
		else
		{
			hashCode =super.hashCode();
		}
		return hashCode;
	}
}
