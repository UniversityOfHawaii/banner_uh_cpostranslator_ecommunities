package edu.hawaii.banner.cpos;
import org.springframework.stereotype.Component;

// simple bean to store the existence of STAR connection details
// see ContextRefreshedListener
@Component
public class PropertyInitializedBean {
    private Boolean starDetailsFound = false;
		private Boolean ctxClosed = false;

    public Boolean getStarDetailsFound() {
			return starDetailsFound;
		}

		public void setStarDetailsFound(Boolean starDetailsFound) {
			this.starDetailsFound = starDetailsFound;
		}

		public Boolean getCtxClosed() {
			return ctxClosed;
		}

		public void setCtxClosed(Boolean ctxClosed) {
			this.ctxClosed = ctxClosed;
		}

}